package com.chinmay.catalogue.feature.home

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.FakeCartRepository
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import kotlinx.coroutines.test.runTest
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()
    private val auth = FakeAuthRepository()
    private val carts = FakeCartRepository(auth)

    @Test
    fun `shows the five featured products in the chosen order`() {
        val viewModel = HomeViewModel(repository, auth, carts)

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(TestData.home.featured.map { it.id }, state.home.featured.map { it.id })
        assertEquals(5, state.home.featured.size)
    }

    @Test
    fun `shows an error, then recovers on retry`() {
        repository.failWith = DataError.Network
        val viewModel = HomeViewModel(repository, auth, carts)
        assertEquals(HomeUiState.Error(DataError.Network), viewModel.uiState.value)

        repository.failWith = null
        viewModel.retry()

        assertTrue(viewModel.uiState.value is HomeUiState.Success)
    }

    @Test
    fun `a failed refresh keeps the products on screen`() {
        val viewModel = HomeViewModel(repository, auth, carts)
        repository.failWith = DataError.Network

        viewModel.refresh()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(false, state.isRefreshing)
        assertEquals(5, state.home.featured.size)
    }

    @Test
    fun `the header shows the signed-in user and their cart count, and clears on sign out`() = runTest {
        val viewModel = HomeViewModel(repository, auth, carts)
        assertEquals(HomeAccount(), viewModel.account.value)

        auth.signIn()
        carts.add("3", 2)
        assertEquals(HomeAccount(TestData.user, cartCount = 2), viewModel.account.value)

        viewModel.signOut()
        assertEquals(HomeAccount(), viewModel.account.value)
    }
}
