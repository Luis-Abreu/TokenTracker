package co.ready.candidateassessment.data.api

import okhttp3.Interceptor
import okhttp3.Response

internal class RateLimitInterceptor(private val maxCallsPerSecond: Int) : Interceptor {

    private val tokens = mutableListOf<Long>()
    private val windowMillis = 1000

    @Synchronized
    override fun intercept(chain: Interceptor.Chain): Response {
        val now = System.currentTimeMillis()

        // Remove tokens older than the time window
        tokens.removeAll { it < now - windowMillis }

        // If we've hit the rate limit, wait until the oldest token expires
        if (tokens.size >= maxCallsPerSecond) {
            val oldestToken = tokens.first()
            val waitTime = (oldestToken + windowMillis) - now
            if (waitTime > 0) {
                Thread.sleep(waitTime)
                // After sleeping, clean up old tokens again
                val newNow = System.currentTimeMillis()
                tokens.removeAll { it < newNow - windowMillis }
            }
        }

        // Add current request token
        tokens.add(System.currentTimeMillis())

        return chain.proceed(chain.request())
    }
}
