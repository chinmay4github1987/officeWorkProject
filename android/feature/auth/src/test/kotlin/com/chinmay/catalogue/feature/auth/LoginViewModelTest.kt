package com.chinmay.catalogue.feature.auth

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = LoginViewModel(auth)

    @Test
    fun `empty fields are flagged without calling the server`() {
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.usernameMissing && state.passwordMissing)
        assertNull(auth.currentUser.value)
    }

    @Test
    fun `right credentials sign in and clear the password`() {
        viewModel.onUsernameChange(TestData.user.username)
        viewModel.onPasswordChange(TestData.PASSWORD)
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.signedIn)
        assertEquals("", state.password)
        assertEquals(TestData.user, auth.currentUser.value)
    }

    @Test
    fun `wrong credentials show an error, keep the username and clear the password`() {
        viewModel.onUsernameChange("asha")
        viewModel.onPasswordChange("wrong")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(DataError.InvalidCredentials, state.error)
        assertEquals("asha", state.username)
        assertEquals("", state.password)

        viewModel.onPasswordChange("x")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `offline keeps the password so the user can just retry`() {
        auth.failWith = DataError.Network
        viewModel.useDemoAccount("asha", TestData.PASSWORD)
        viewModel.onSubmit()

        assertEquals(DataError.Network, viewModel.uiState.value.error)
        assertEquals(TestData.PASSWORD, viewModel.uiState.value.password)
    }
}
