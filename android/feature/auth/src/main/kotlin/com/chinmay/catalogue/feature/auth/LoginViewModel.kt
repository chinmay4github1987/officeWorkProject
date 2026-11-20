package com.chinmay.catalogue.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A form, like the enquiry form: one state object, with errors next to the fields they belong to. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val usernameMissing: Boolean = false,
    val passwordMissing: Boolean = false,
    val isSubmitting: Boolean = false,
    /** Wrong credentials, offline, server down. Shown as a banner; the typed username stays. */
    val error: DataError? = null,
    /** Set once signed in: the screen closes itself. */
    val signedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) =
        _uiState.update { it.copy(username = value, usernameMissing = false, error = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordMissing = false, error = null) }

    fun onTogglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    /** Debug builds offer a one-tap demo account. */
    fun useDemoAccount(username: String, password: String) =
        _uiState.update { it.copy(username = username, password = password, usernameMissing = false, passwordMissing = false, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting || state.signedIn) return
        val usernameMissing = state.username.isBlank()
        val passwordMissing = state.password.isEmpty()
        if (usernameMissing || passwordMissing) {
            _uiState.update { it.copy(usernameMissing = usernameMissing, passwordMissing = passwordMissing) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = auth.login(state.username, state.password)) {
                is DataResult.Success -> _uiState.update { it.copy(isSubmitting = false, signedIn = true, password = "") }
                // A wrong password is cleared so the next attempt starts fresh; the username stays.
                is DataResult.Failure -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = result.error,
                        password = if (result.error == DataError.InvalidCredentials) "" else it.password,
                    )
                }
            }
        }
    }
}
