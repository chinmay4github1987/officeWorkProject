package com.chinmay.catalogue.feature.home

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
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

    @Test
    fun `shows the five featured products in the chosen order`() {
        val viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(TestData.home.featured.map { it.id }, state.home.featured.map { it.id })
        assertEquals(5, state.home.featured.size)
    }

    @Test
    fun `shows an error, then recovers on retry`() {
        repository.failWith = DataError.Network
        val viewModel = HomeViewModel(repository)
        assertEquals(HomeUiState.Error(DataError.Network), viewModel.uiState.value)

        repository.failWith = null
        viewModel.retry()

        assertTrue(viewModel.uiState.value is HomeUiState.Success)
    }

    @Test
    fun `a failed refresh keeps the products on screen`() {
        val viewModel = HomeViewModel(repository)
        repository.failWith = DataError.Network

        viewModel.refresh()

        val state = viewModel.uiState.value as HomeUiState.Success
        assertEquals(false, state.isRefreshing)
        assertEquals(5, state.home.featured.size)
    }
}
