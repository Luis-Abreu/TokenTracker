package co.ready.candidateassessment.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import co.ready.candidateassessment.domain.Token
import co.ready.candidateassessment.domain.TokenPrice

@Entity(tableName = "tokens")
internal data class TokenEntity(
    @PrimaryKey
    val address: String,
    val name: String,
    val symbol: String,
    val decimals: Int,
    val image: String,
    val priceRate: Double?,
    val priceCurrency: String?,
    val priceDiff: Double?,
    val priceMarketCapUsd: Double?,
    val priceVolume24h: Double?,
    val holdersCount: Long?,
    val totalSupply: String?,
    val orderIndex: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Token = Token(
        address = address,
        name = name,
        symbol = symbol,
        decimals = decimals,
        image = image,
        price = if (priceRate != null && priceCurrency != null) {
            TokenPrice(
                rate = priceRate,
                currency = priceCurrency,
                diff = priceDiff,
                marketCapUsd = priceMarketCapUsd,
                volume24h = priceVolume24h
            )
        } else {
            null
        },
        holdersCount = holdersCount,
        totalSupply = totalSupply
    )

    companion object {
        fun fromDomain(token: Token, orderIndex: Int = 0): TokenEntity = TokenEntity(
            address = token.address,
            name = token.name,
            symbol = token.symbol,
            decimals = token.decimals,
            image = token.image,
            priceRate = token.price?.rate,
            priceCurrency = token.price?.currency,
            priceDiff = token.price?.diff,
            priceMarketCapUsd = token.price?.marketCapUsd,
            priceVolume24h = token.price?.volume24h,
            holdersCount = token.holdersCount,
            totalSupply = token.totalSupply,
            orderIndex = orderIndex
        )
    }
}
