package co.ready.candidateassessment.data.api

import retrofit2.http.GET
import retrofit2.http.Query

internal interface EtherscanApi {

    @GET("/v2/api?chainid=1&module=account&action=tokenbalance&tag=latest")
    suspend fun getTokenBalance(
        @Query("contractaddress") contractAddress: String,
        @Query("address") address: String,
        @Query("apiKey") apiKey: String
    ): TokenBalanceResponse

    data class TokenBalanceResponse(val status: Long, val message: String, val result: String)
}
