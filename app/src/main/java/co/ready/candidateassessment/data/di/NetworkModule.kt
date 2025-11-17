package co.ready.candidateassessment.data.di

import co.ready.candidateassessment.data.api.EtherscanApi
import co.ready.candidateassessment.data.api.EthplorerApi
import co.ready.candidateassessment.data.api.PriceInfoAdapter
import co.ready.candidateassessment.data.api.RateLimitInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    private const val ETHERSCAN_BASE_URL = "https://api.etherscan.io/"
    private const val ETHPLORER_BASE_URL = "https://api.ethplorer.io/"
    private const val ETHERSCAN_RATE_LIMIT = 4

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Provides
    @Singleton
    fun provideMoshiConverter(): MoshiConverterFactory {
        val moshi = Moshi.Builder()
            .add(PriceInfoAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        return MoshiConverterFactory.create(moshi)
    }

    @Provides
    @Singleton
    fun provideEtherscanApiClient(moshiConverter: MoshiConverterFactory): EtherscanApi = Retrofit.Builder()
        .baseUrl(ETHERSCAN_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }
                )
                .addInterceptor(RateLimitInterceptor(ETHERSCAN_RATE_LIMIT))
                .build()
        )
        .addConverterFactory(moshiConverter)
        .build()
        .create(EtherscanApi::class.java)

    @Provides
    @Singleton
    fun provideEthExplorerApiClient(moshiConverter: MoshiConverterFactory): EthplorerApi = Retrofit.Builder()
        .baseUrl(ETHPLORER_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }
                )
                .build()
        )
        .addConverterFactory(moshiConverter)
        .build()
        .create(EthplorerApi::class.java)
}
