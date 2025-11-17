package co.ready.candidateassessment.data.di

import android.content.Context
import androidx.room.Room
import co.ready.candidateassessment.data.cache.TokenBalanceDao
import co.ready.candidateassessment.data.cache.TokenDao
import co.ready.candidateassessment.data.cache.TokenDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideTokenDatabase(@ApplicationContext context: Context): TokenDatabase = Room.databaseBuilder(
        context,
        TokenDatabase::class.java,
        "token_database"
    )
        .fallbackToDestructiveMigration(true)
        .build()

    @Provides
    @Singleton
    fun provideTokenDao(database: TokenDatabase): TokenDao = database.tokenDao()

    @Provides
    @Singleton
    fun provideTokenBalanceDao(database: TokenDatabase): TokenBalanceDao = database.tokenBalanceDao()
}
