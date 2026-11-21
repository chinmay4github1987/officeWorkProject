package com.chinmay.catalogue.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.RecentSearchesRepository
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** What the results area shows. The text field's content is separate (it changes on every keystroke). */
sealed interface SearchResults {
    /** Nothing searched yet. */
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Found(val term: String, val products: List<Product>) : SearchResults
    data class Error(val error: DataError) : SearchResults
}

data class SearchUiState(
    val query: String = "",
    val showRequiredError: Boolean = false,
    val results: SearchResults = SearchResults.Idle,
    /** Category chip the results are narrowed to; null = "All". */
    val selectedCategory: String? = null,
    /** This phone's last searches, newest first. */
    val recent: List<String> = emptyList(),
    /** Product names and categories matching what's being typed, before Search is pressed. */
    val suggestions: List<String> = emptyList(),
    /** How many results are visible; "Load more" shows another page. */
    val shownCount: Int = PAGE_SIZE,
) {
    /** The categories in the current results, in the order they first appear: one chip each. */
    val categories: List<Category>
        get() = (results as? SearchResults.Found)?.products?.map { it.category }?.distinctBy { it.slug }.orEmpty()

    /** The results narrowed to the selected category chip (all of them, before paging). */
    val filteredProducts: List<Product>
        get() = (results as? SearchResults.Found)?.products
            ?.filter { selectedCategory == null || it.category.slug == selectedCategory }
            .orEmpty()

    /** What the grid shows now: the filtered results, one page at a time. */
    val visibleProducts: List<Product> get() = filteredProducts.take(shownCount)

    val remaining: Int get() = (filteredProducts.size - shownCount).coerceAtLeast(0)

    companion object {
        const val PAGE_SIZE = 10
        const val MAX_SUGGESTIONS = 5
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
    private val recentSearches: RecentSearchesRepository,
) : ViewModel() {

    // The typed text survives process death, so the user doesn't lose it if Android kills the app.
    private val _uiState = MutableStateFlow(SearchUiState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestJob: Job? = null

    init {
        viewModelScope.launch { recentSearches.recent.collect { recent -> _uiState.update { it.copy(recent = recent) } } }
    }

    fun onQueryChange(query: String) {
        savedStateHandle[QUERY_KEY] = query
        _uiState.update { it.copy(query = query, showRequiredError = false) }
        suggest(query)
    }

    /**
     * Runs on the Search button (or the keyboard's search key), not on every keystroke. The term is trimmed;
     * a blank one is caught here without calling the server. Only products matching every word are shown.
     */
    fun onSearch() {
        val term = _uiState.value.query.trim()
        if (term.isEmpty()) {
            _uiState.update { it.copy(showRequiredError = true) }
            return
        }
        suggestJob?.cancel()
        searchJob?.cancel() // a newer search replaces an older one still in flight
        recentSearches.add(term)
        _uiState.update {
            it.copy(results = SearchResults.Loading, selectedCategory = null, suggestions = emptyList(), shownCount = SearchUiState.PAGE_SIZE)
        }
        searchJob = viewModelScope.launch {
            val results = when (val result = repository.search(term)) {
                is DataResult.Success -> SearchResults.Found(term, result.data)
                is DataResult.Failure -> SearchResults.Error(result.error)
            }
            _uiState.update { it.copy(results = results) }
        }
    }

    /** A recent search or suggestion: the same as typing it and pressing Search. */
    fun onSearchFor(term: String) {
        onQueryChange(term)
        onSearch()
    }

    /** Narrows the results to one category (null = All). Filters what's already loaded: no new request. */
    fun onCategorySelected(slug: String?) {
        _uiState.update { it.copy(selectedCategory = slug, shownCount = SearchUiState.PAGE_SIZE) }
    }

    fun onLoadMore() {
        _uiState.update { if (it.remaining > 0) it.copy(shownCount = it.shownCount + SearchUiState.PAGE_SIZE) else it }
    }

    fun onClearRecent() = recentSearches.clear()

    fun onClear() {
        searchJob?.cancel()
        suggestJob?.cancel()
        savedStateHandle[QUERY_KEY] = ""
        _uiState.value = SearchUiState(recent = _uiState.value.recent)
    }

    /**
     * Up to five matching product names and category names for what's typed (2+ letters). They come from the
     * catalogue already in memory, so typing doesn't send a request per keystroke.
     */
    private fun suggest(query: String) {
        suggestJob?.cancel()
        val term = query.trim()
        val lastSearched = (_uiState.value.results as? SearchResults.Found)?.term
        if (term.length < 2 || term.equals(lastSearched, ignoreCase = true)) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            val products = (repository.search(term) as? DataResult.Success)?.data.orEmpty()
            val needle = term.lowercase(Locale.ROOT)
            val categories = products.map { it.category.name }.distinct().filter { needle in it.lowercase(Locale.ROOT) }
            val titles = products.map { it.title }.filter { needle in it.lowercase(Locale.ROOT) }
            val suggestions = (categories + titles).distinct().take(SearchUiState.MAX_SUGGESTIONS)
            _uiState.update { if (it.query.trim() == term) it.copy(suggestions = suggestions) else it }
        }
    }

    private companion object {
        const val QUERY_KEY = "query"
    }
}
