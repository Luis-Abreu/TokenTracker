package co.ready.candidateassessment.data.di

import co.ready.candidateassessment.data.TokenRepository
import co.ready.candidateassessment.domain.TokenService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTokenService(tokenRepository: TokenRepository): TokenService
}
