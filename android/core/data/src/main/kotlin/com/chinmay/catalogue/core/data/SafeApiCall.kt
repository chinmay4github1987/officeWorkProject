package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.network.dto.ErrorEnvelopeDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a network call and turns every failure into a typed [DataError].
 * This is the only place that knows about HTTP status codes and exceptions.
 */
internal suspend fun <T> safeApiCall(json: Json, block: suspend () -> T): DataResult<T> = try {
    DataResult.Success(block())
} catch (e: CancellationException) {
    // Never swallow cancellation: it is how coroutines stop work when a screen closes.
    throw e
} catch (e: HttpException) {
    DataResult.Failure(e.toDataError(json))
} catch (e: IOException) {
    DataResult.Failure(DataError.Network)
} catch (e: SerializationException) {
    DataResult.Failure(DataError.Server(message = null))
}

private fun HttpException.toDataError(json: Json): DataError {
    val body = response()?.errorBody()?.string()
    val error = body?.let { runCatching { json.decodeFromString<ErrorEnvelopeDto>(it).error }.getOrNull() }
    return when (code()) {
        400 -> if (error?.fields?.isNotEmpty() == true) DataError.Validation(error.fields) else DataError.Server(error?.message)
        404 -> DataError.NotFound
        429 -> DataError.RateLimited
        else -> DataError.Server(error?.message)
    }
}
