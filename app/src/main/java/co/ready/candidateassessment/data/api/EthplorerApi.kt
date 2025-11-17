package co.ready.candidateassessment.data.api

import retrofit2.http.GET
import retrofit2.http.Query

internal interface EthplorerApi {

    @GET("/getTopTokens")
    suspend fun getTopTokens(@Query("limit") limit: Int = 50, @Query("apiKey") apiKey: String): TopTokensResponse

    data class TopTokensResponse(val tokens: List<TokenResponse>)

    data class TokenResponse(
        val address: String,
        val name: String?,
        val symbol: String?,
        val decimals: String?,
        val image: String?,
        val price: PriceInfo?,
        val holdersCount: Long?,
        val totalSupply: String?
    )

    data class PriceInfo(
        val rate: Double?,
        val currency: String?,
        val diff: Double?,
        val marketCapUsd: Double?,
        val volume24h: Double?
    )
}
