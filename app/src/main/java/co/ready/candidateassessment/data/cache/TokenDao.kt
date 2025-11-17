package co.ready.candidateassessment.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface TokenDao {

    @Query("SELECT * FROM tokens ORDER BY orderIndex ASC")
    suspend fun getAllTokens(): List<TokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokens(tokens: List<TokenEntity>)

    @Query("DELETE FROM tokens")
    suspend fun clearTokens()

    @Query("SELECT cachedAt FROM tokens ORDER BY cachedAt DESC LIMIT 1")
    suspend fun getTokensCacheTimestamp(): Long?
}
