package com.chinmay.catalogue.feature.search

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.FakeRecentSearchesRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()
    private val recent = FakeRecentSearchesRepository()
    private val viewModel by lazy { SearchViewModel(SavedStateHandle(), repository, recent) }

    @Test
    fun `typing shows suggestions but no results, tapping Search shows the results`() {
        viewModel.onQueryChange("ssd")
        assertEquals(SearchResults.Idle, viewModel.uiState.value.results)
        assertEquals(listOf("SanDisk SSD", "Silicon Power SSD"), viewModel.uiState.value.suggestions)

        viewModel.onSearch()

        val results = viewModel.uiState.value.results as SearchResults.Found
        assertEquals(listOf("SanDisk SSD", "Silicon Power SSD"), results.products.map { it.title })
        assertEquals("ssd", repository.searchTerms.last()) // the search itself, after the suggestion lookup
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
    fun `category chips narrow the results without a new request, and a new search resets them`() {
        viewModel.onQueryChange("o") // Cotton Jacket (men's), Portable Hard Drive and Silicon Power SSD (electronics)
        viewModel.onSearch()
        assertEquals(listOf("mens-clothing", "electronics"), viewModel.uiState.value.categories.map { it.slug })

        viewModel.onCategorySelected("electronics")

        assertEquals(listOf("Portable Hard Drive", "Silicon Power SSD"), viewModel.uiState.value.visibleProducts.map { it.title })
        assertEquals(1, repository.searchTerms.size)

        viewModel.onCategorySelected(null)
        assertEquals(3, viewModel.uiState.value.visibleProducts.size)

        viewModel.onCategorySelected("mens-clothing")
        viewModel.onSearch()
        assertEquals(null, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `the typed query is restored after process death`() {
        val restored = SearchViewModel(SavedStateHandle(mapOf("query" to "jacket")), repository, recent)
        assertEquals("jacket", restored.uiState.value.query)
    }

    @Test
    fun `searches are remembered, newest first, and can be cleared`() {
        viewModel.onSearchFor("ssd")
        viewModel.onSearchFor("  Jacket ")
        assertEquals(listOf("Jacket", "ssd"), viewModel.uiState.value.recent)

        viewModel.onClearRecent()
        assertTrue(viewModel.uiState.value.recent.isEmpty())
    }

    @Test
    fun `picking a suggestion searches for it and hides the suggestions`() {
        viewModel.onQueryChange("sand")
        assertEquals(listOf("SanDisk SSD"), viewModel.uiState.value.suggestions)

        viewModel.onSearchFor("SanDisk SSD")

        val state = viewModel.uiState.value
        assertTrue(state.suggestions.isEmpty())
        assertEquals(listOf("SanDisk SSD"), (state.results as SearchResults.Found).products.map { it.title })
    }

    @Test
    fun `results show ten at a time`() {
        repository.products = (1..23).map { com.chinmay.catalogue.core.testing.TestData.product(it.toString(), "Lamp $it") }
        viewModel.onSearchFor("lamp")

        assertEquals(10, viewModel.uiState.value.visibleProducts.size)
        viewModel.onLoadMore()
        viewModel.onLoadMore()
        assertEquals(23, viewModel.uiState.value.visibleProducts.size)
        assertEquals(0, viewModel.uiState.value.remaining)
    }

    @Test
    fun `a whitespace-only term is never searched or remembered`() {
        viewModel.onSearchFor("   ")

        assertTrue(viewModel.uiState.value.showRequiredError)
        assertTrue(repository.searchTerms.isEmpty())
        assertTrue(recent.recent.value.isEmpty())
    }
}
