package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.CatalogueApi
import com.chinmay.catalogue.core.network.dto.LoginRequestDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface AuthRepository {
    /** The signed-in customer, or null. Survives app restarts until the session expires or they sign out. */
    val currentUser: StateFlow<User?>

    /** Wrong credentials fail with DataError.InvalidCredentials. */
    suspend fun login(username: String, password: String): DataResult<User>

    fun logout()
}

internal class NetworkAuthRepository @Inject constructor(
    private val api: CatalogueApi,
    private val json: Json,
    private val session: SessionStore,
) : AuthRepository {

    override val currentUser: StateFlow<User?> = session.user

    override suspend fun login(username: String, password: String): DataResult<User> {
        val result = safeApiCall(json) { api.login(LoginRequestDto(username.trim(), password)) }
        return when (result) {
            is DataResult.Success -> {
                val user = result.data.user.asExternalModel()
                session.save(result.data.token, user)
                DataResult.Success(user)
            }
            is DataResult.Failure -> result
        }
    }

    /** Session tokens are stateless and expire on their own, so signing out only forgets the token here. */
    override fun logout() = session.clear()
}
