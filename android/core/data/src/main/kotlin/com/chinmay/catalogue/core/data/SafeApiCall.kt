package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a network call and turns every failure into a typed [DataError].
 * This is the only place that knows about HTTP status codes and exceptions. Fake Store's error bodies are plain
 * text ("username or password is incorrect"), so only the status code is used; callers refine it where the
 * meaning depends on the endpoint (a 401 from login means wrong credentials).
 */
internal suspend fun <T> safeApiCall(block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: CancellationException) {
    // Never swallow cancellation: it is how coroutines stop work when a screen closes.
    throw e
} catch (e: HttpException) {
    DataResult.Failure(httpError(e.code()))
} catch (e: IOException) {
    DataResult.Failure(DataError.Network)
} catch (e: SerializationException) {
    // The service answered, but not with the JSON we expect.
    DataResult.Failure(DataError.Server(message = null))
}

/** What an HTTP error status means to a screen. */
internal fun httpError(status: Int): DataError = when (status) {
    401, 403 -> DataError.Unauthorized
    404 -> DataError.NotFound
    429 -> DataError.RateLimited
    else -> DataError.Server(message = null)
}
