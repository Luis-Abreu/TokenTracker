package co.ready.candidateassessment.common

import android.util.Log.e
import com.squareup.moshi.JsonDataException
import retrofit2.HttpException
import java.io.IOException

sealed class Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>()
    data class Error(val message: String, val code: Int? = null, val exception: Throwable? = null) : Outcome<Nothing>()

    object Loading : Outcome<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): Outcome<T> = try {
    val result = apiCall()
    Outcome.Success(result)
} catch (e: HttpException) {
    val errorBody = e.response()?.errorBody()?.string()
    e("SafeApiCall", "HTTP error: ${e.code()}", e)
    Outcome.Error(
        message = "HTTP ${e.code()}: ${errorBody ?: e.message()}",
        code = e.code(),
        exception = e
    )
} catch (e: IOException) {
    e("SafeApiCall", "Network error", e)
    Outcome.Error(
        message = "Network connection failed. Please check your internet connection.",
        exception = e
    )
} catch (e: JsonDataException) {
    e("SafeApiCall", "JSON parsing error: ${e.message}", e)
    Outcome.Error(
        message = "Failed to parse server response: ${e.message}",
        exception = e
    )
} catch (e: Exception) {
    e("SafeApiCall", "Unexpected error: ${e.message}", e)
    Outcome.Error(
        message = "An unexpected error occurred: ${e.localizedMessage}",
        exception = e
    )
}

suspend fun <T> Outcome<T>.onSuccess(action: suspend (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) {
        action(data)
    }
    return this
}

suspend fun <T> Outcome<T>.onError(action: suspend (String, Int?, Throwable?) -> Unit): Outcome<T> {
    if (this is Outcome.Error) {
        action(message, code, exception)
    }
    return this
}

suspend fun <T> Outcome<T>.onLoading(action: suspend () -> Unit): Outcome<T> {
    if (this is Outcome.Loading) {
        action()
    }
    return this
}

suspend fun <T, R> Outcome<T>.mapSuccess(transform: suspend (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> {
        try {
            Outcome.Success(transform(data))
        } catch (e: Exception) {
            Outcome.Error(
                message = e.message ?: "Failed to transform data",
                exception = e
            )
        }
    }
    is Outcome.Error -> this
    is Outcome.Loading -> this
}

fun <T> Outcome<T>.getOrNull(): T? = when (this) {
    is Outcome.Success -> data
    else -> null
}

fun <T> Outcome<T>.getOrDefault(defaultValue: T): T = when (this) {
    is Outcome.Success -> data
    else -> defaultValue
}

fun <T> Outcome<T>.getOrThrow(): T {
    when (this) {
        is Outcome.Success -> return data
        is Outcome.Error -> throw exception ?: Throwable(message)
        is Outcome.Loading -> throw IllegalStateException("Data is still loading")
    }
}
