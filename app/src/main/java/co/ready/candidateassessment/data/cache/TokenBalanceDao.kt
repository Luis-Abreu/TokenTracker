package co.ready.candidateassessment.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface TokenBalanceDao {

    @Query("SELECT * FROM token_balances WHERE tokenAddress = :tokenAddress")
    suspend fun getBalance(tokenAddress: String): TokenBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: TokenBalanceEntity)

    @Query("DELETE FROM token_balances")
    suspend fun clearBalances()

    @Query("SELECT cachedAt FROM token_balances WHERE tokenAddress = :tokenAddress")
    suspend fun getBalanceCacheTimestamp(tokenAddress: String): Long?
}
