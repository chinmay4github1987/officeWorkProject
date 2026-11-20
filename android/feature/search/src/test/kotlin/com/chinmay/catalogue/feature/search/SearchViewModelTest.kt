package com.chinmay.catalogue.feature.search

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()
    private val viewModel by lazy { SearchViewModel(SavedStateHandle(), repository) }

    @Test
    fun `typing does not search, tapping Search does`() {
        viewModel.onQueryChange("ssd")
        assertEquals(SearchResults.Idle, viewModel.uiState.value.results)
        assertTrue(repository.searchTerms.isEmpty())

        viewModel.onSearch()

        val results = viewModel.uiState.value.results as SearchResults.Found
        assertEquals(listOf("SanDisk SSD", "Silicon Power SSD"), results.products.map { it.title })
        assertEquals(listOf("ssd"), repository.searchTerms)
    }

    @Test
    fun `a blank term shows a message and never calls the server`() {
        viewModel.onQueryChange("   ")
        viewModel.onSearch()

        assertTrue(viewModel.uiState.value.showRequiredError)
        assertTrue(repository.searchTerms.isEmpty())
    }

    @Test
    fun `no matches is an empty result, not an error`() {
        viewModel.onQueryChange("unicorn")
        viewModel.onSearch()

        assertEquals(SearchResults.Found("unicorn", emptyList()), viewModel.uiState.value.results)
    }

    @Test
    fun `a failed search shows the error`() {
        repository.failWith = DataError.Network
        viewModel.onQueryChange("ssd")
        viewModel.onSearch()

        assertEquals(SearchResults.Error(DataError.Network), viewModel.uiState.value.results)
    }

    @Test
    fun `the typed query is restored after process death`() {
        val restored = SearchViewModel(SavedStateHandle(mapOf("query" to "jacket")), repository)
        assertEquals("jacket", restored.uiState.value.query)
    }
}
