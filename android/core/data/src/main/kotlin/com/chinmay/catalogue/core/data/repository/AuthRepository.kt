package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.FakeStoreApi
import com.chinmay.catalogue.core.network.dto.FakeStoreLoginRequestDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.fakeStoreUserIdFromToken
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface AuthRepository {
    /** The signed-in customer, or null. Survives app restarts until they sign out. */
    val currentUser: StateFlow<User?>

    /** Wrong credentials fail with DataError.InvalidCredentials. */
    suspend fun login(username: String, password: String): DataResult<User>

    fun logout()
}

/**
 * Signs in with Fake Store's POST /auth/login. Its token carries the user's ID, so only that user's profile is loaded
 * (GET /users/{id}); the password and address Fake Store includes there are never parsed or kept.
 */
internal class FakeStoreAuthRepository @Inject constructor(
    private val api: FakeStoreApi,
    private val json: Json,
    private val session: SessionStore,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = session.user

    override suspend fun login(username: String, password: String): DataResult<User> {
        val token = when (val result = safeApiCall { api.login(FakeStoreLoginRequestDto(username.trim(), password)).token }) {
            is DataResult.Success -> result.data
            // Fake Store answers wrong credentials with 401. (Missing fields would be 400; the form never sends those.)
            // Anything else, e.g. Fake Store being down, keeps its own error: it isn't the user's password.
            is DataResult.Failure -> return DataResult.Failure(
                if (result.error == DataError.Unauthorized) DataError.InvalidCredentials else result.error,
            )
        }
        val userId = fakeStoreUserIdFromToken(token, json) ?: return DataResult.Failure(DataError.Server(message = null))
        return when (val profile = safeApiCall { api.user(userId).asExternalModel() }) {
            is DataResult.Success -> {
                session.save(token, profile.data)
                profile
            }
            is DataResult.Failure -> profile
        }
    }

    /** Fake Store tokens are never sent anywhere, so signing out only forgets the session on this phone. */
    override fun logout() = session.clear()
}
