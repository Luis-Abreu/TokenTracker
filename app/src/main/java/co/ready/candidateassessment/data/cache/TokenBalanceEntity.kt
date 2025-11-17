package co.ready.candidateassessment.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_balances")
internal data class TokenBalanceEntity(
    @PrimaryKey
    val tokenAddress: String,
    val balance: String,
    val cachedAt: Long = System.currentTimeMillis()
)
