package com.chinmay.catalogue.core.data.session

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import kotlinx.serialization.json.Json

/**
 * [safeApiCall] for endpoints that need a session. Signed out: fails fast without a request.
 * The server says the session is no longer valid: signs out, so every screen sees the same state.
 */
internal suspend fun <T> SessionStore.authenticated(json: Json, block: suspend () -> T): DataResult<T> {
    if (user.value == null) return DataResult.Failure(DataError.Unauthorized)
    val result = safeApiCall(json, block)
    if (result is DataResult.Failure && result.error == DataError.Unauthorized) clear()
    return result
}
