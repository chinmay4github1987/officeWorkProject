package com.chinmay.catalogue.core.data.session

import android.content.Context
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.dto.UserDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.asNetworkModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** The one place that knows whether someone is signed in. */
internal interface SessionStore {
    val user: StateFlow<User?>
    fun token(): String?
    fun save(token: String, user: User)
    fun clear()
}

/**
 * Keeps the session in app-private SharedPreferences so sign-in survives restarts.
 * The token is Fake Store's login token (never a password); it is kept but never sent anywhere. The app
 * has `allowBackup="false"`, so it doesn't leave the device. Read once at startup, then served from memory.
 */
@Singleton
internal class SharedPreferencesSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) : SessionStore {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    @Volatile
    private var token: String? = prefs.getString(KEY_TOKEN, null)

    private val _user = MutableStateFlow(
        if (token == null) null else prefs.getString(KEY_USER, null)?.let { decode(it) },
    )
    override val user: StateFlow<User?> = _user.asStateFlow()

    init {
        // A token without a readable user (e.g. an older app version) is treated as signed out.
        if (token != null && _user.value == null) clear()
    }

    override fun token(): String? = token

    override fun save(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, json.encodeToString(UserDto.serializer(), user.asNetworkModel()))
            .apply()
        this.token = token
        _user.value = user
    }

    override fun clear() {
        prefs.edit().clear().apply()
        token = null
        _user.value = null
    }

    private fun decode(raw: String): User? =
        runCatching { json.decodeFromString(UserDto.serializer(), raw).asExternalModel() }.getOrNull()

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}
