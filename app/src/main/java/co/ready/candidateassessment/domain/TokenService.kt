package co.ready.candidateassessment.domain

import co.ready.candidateassessment.common.Outcome
import kotlinx.coroutines.flow.Flow

interface TokenService {
    fun getTopTokens(forceRefresh: Boolean = false): Flow<Outcome<List<Token>>>
    fun getTokenBalance(tokenContractAddress: String, forceRefresh: Boolean = false): Flow<Outcome<String>>
    suspend fun clearCache()
}

data class Token(
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val image: String,
    val price: TokenPrice?,
    val holdersCount: Long?,
    val totalSupply: String?
)

data class TokenPrice(
    val rate: Double,
    val currency: String,
    val diff: Double?,
    val marketCapUsd: Double?,
    val volume24h: Double?
)
