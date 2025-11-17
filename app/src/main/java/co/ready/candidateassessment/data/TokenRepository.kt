package co.ready.candidateassessment.data

import android.util.Log
import co.ready.candidateassessment.app.Constants
import co.ready.candidateassessment.common.Outcome
import co.ready.candidateassessment.common.mapSuccess
import co.ready.candidateassessment.common.safeApiCall
import co.ready.candidateassessment.data.api.EtherscanApi
import co.ready.candidateassessment.data.api.EthplorerApi
import co.ready.candidateassessment.data.cache.TokenBalanceDao
import co.ready.candidateassessment.data.cache.TokenBalanceEntity
import co.ready.candidateassessment.data.cache.TokenDao
import co.ready.candidateassessment.data.cache.TokenEntity
import co.ready.candidateassessment.domain.Token
import co.ready.candidateassessment.domain.TokenPrice
import co.ready.candidateassessment.domain.TokenService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.bouncycastle.jcajce.provider.digest.Keccak
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TokenRepository @Inject constructor(
    private val ethplorerApi: EthplorerApi,
    private val etherscanApi: EtherscanApi,
    private val tokenDao: TokenDao,
    private val tokenBalanceDao: TokenBalanceDao,
) : TokenService {

    override fun getTopTokens(forceRefresh: Boolean): Flow<Outcome<List<Token>>> = flow {
        if (!forceRefresh) {
            // first, emit cached data if available
            val cachedTokens = tokenDao.getAllTokens()
            if (cachedTokens.isNotEmpty()) {
                emit(Outcome.Success(cachedTokens.map { it.toDomain() }))
            } else {
                // emit loading state if no cache
                emit(Outcome.Loading)
            }
        } else {
            // when forcing refresh, ignore the cache and always emit loading first
            emit(Outcome.Loading)
        }

        // fetch from network
        val networkResult = safeApiCall {
            ethplorerApi.getTopTokens(
                apiKey = Constants.ethExplorerApiKey
            )
        }.mapSuccess { tokensResponse ->
            tokensResponse.tokens.map { it.toDomain() }
        }

        // cache and emit the results
        when (networkResult) {
            is Outcome.Success -> {
                val tokenEntities = networkResult.data.mapIndexed { index, token ->
                    TokenEntity.fromDomain(token, index)
                }
                tokenDao.insertTokens(tokenEntities)
                emit(networkResult)
            }

            is Outcome.Error -> {
                emit(networkResult)
            }

            Outcome.Loading -> {
                // already emitted loading if needed
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun EthplorerApi.TokenResponse.toDomain(): Token = Token(
        address = address,
        name = name ?: "",
        symbol = symbol ?: "",
        decimals = decimals?.toIntOrNull() ?: 0,
        image = getTokenImageUrl(address),
        price = price?.let {
            TokenPrice(
                rate = it.rate ?: 0.0,
                currency = it.currency ?: "USD",
                diff = it.diff,
                marketCapUsd = it.marketCapUsd,
                volume24h = it.volume24h
            )
        },
        holdersCount = holdersCount,
        totalSupply = totalSupply
    )

    private fun getTokenImageUrl(address: String): String = try {
        val checksumAddress = toChecksumAddress(address)
        val url =
            "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/ethereum/assets/$checksumAddress/logo.png"
        Log.d("TokenRepository", "TrustWallet URL for $address -> $checksumAddress: $url")
        url
    } catch (e: Exception) {
        Log.e("TokenRepository", "Error generating TrustWallet URL for $address", e)
        "" // empty string will be handled later
    }

    /**
     * converts an eth address to EIP-55 checksum format.
     */
    private fun toChecksumAddress(address: String): String {
        // remove 0x prefix if present and convert to lowercase
        val cleanAddress = address.removePrefix("0x").lowercase()

        // compute Keccak-256 hash of the lowercase address
        val hash = Keccak.Digest256().digest(cleanAddress.toByteArray())
        val hashHex = hash.joinToString("") { "%02x".format(it) }

        // build checksum address
        val checksumAddress = StringBuilder("0x")
        for (i in cleanAddress.indices) {
            val char = cleanAddress[i]
            // if the character is a letter (a-f), check the hash
            if (char in 'a'..'f') {
                // if the corresponding hash digit is >= 8, uppercase the letter
                val hashDigit = hashHex[i].toString().toInt(16)
                checksumAddress.append(if (hashDigit >= 8) char.uppercaseChar() else char)
            } else {
                // numbers stay as-is
                checksumAddress.append(char)
            }
        }

        return checksumAddress.toString()
    }

    override fun getTokenBalance(tokenContractAddress: String, forceRefresh: Boolean): Flow<Outcome<String>> = flow {
        if (!forceRefresh) {
            // first emit cached balance if available (only when not forcing refresh)
            val cachedBalance = tokenBalanceDao.getBalance(tokenContractAddress)
            if (cachedBalance != null) {
                emit(Outcome.Success(cachedBalance.balance))
            } else {
                emit(Outcome.Loading)
            }
        } else {
            // when forcing refresh, ignore the cache
            emit(Outcome.Loading)
        }

        // fetch from network
        val networkResult = safeApiCall {
            etherscanApi.getTokenBalance(
                contractAddress = tokenContractAddress,
                address = Constants.walletAddress,
                apiKey = Constants.etherscanApiKey
            )
        }.mapSuccess {
            it.result
        }

        // if network call succeeds, cache and emit the result
        when (networkResult) {
            is Outcome.Success -> {
                val balanceEntity = TokenBalanceEntity(
                    tokenAddress = tokenContractAddress,
                    balance = networkResult.data
                )
                tokenBalanceDao.insertBalance(balanceEntity)
                emit(networkResult)
            }

            is Outcome.Error -> {
                // only emit error if we don't have cached data or if forcing refresh
                if (forceRefresh) {
                    emit(networkResult)
                } else {
                    val cachedBalance = tokenBalanceDao.getBalance(tokenContractAddress)
                    // we only emit the error if there's no cached data
                    if (cachedBalance == null) {
                        emit(networkResult)
                    }
                }
            }

            Outcome.Loading -> {
                // already emitted loading if needed
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        tokenDao.clearTokens()
        tokenBalanceDao.clearBalances()
    }
}
