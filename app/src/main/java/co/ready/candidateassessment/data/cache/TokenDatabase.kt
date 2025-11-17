package co.ready.candidateassessment.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TokenEntity::class, TokenBalanceEntity::class],
    version = 3,
    exportSchema = false
)
internal abstract class TokenDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun tokenBalanceDao(): TokenBalanceDao
}
