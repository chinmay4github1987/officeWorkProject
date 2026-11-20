package com.chinmay.catalogue.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
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
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
) : ViewModel() {

    // The typed text survives process death, so the user doesn't lose it if Android kills the app.
    private val _uiState = MutableStateFlow(SearchUiState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        savedStateHandle[QUERY_KEY] = query
        _uiState.update { it.copy(query = query, showRequiredError = false) }
    }

    /**
     * Runs on the Search button (or the keyboard's search key), as the acceptance criteria describe,
     * not on every keystroke. A blank term is caught here without calling the server.
     */
    fun onSearch() {
        val term = _uiState.value.query.trim()
        if (term.isEmpty()) {
            _uiState.update { it.copy(showRequiredError = true) }
            return
        }
        searchJob?.cancel() // a newer search replaces an older one still in flight
        _uiState.update { it.copy(results = SearchResults.Loading) }
        searchJob = viewModelScope.launch {
            val results = when (val result = repository.search(term)) {
                is DataResult.Success -> SearchResults.Found(term, result.data)
                is DataResult.Failure -> SearchResults.Error(result.error)
            }
            _uiState.update { it.copy(results = results) }
        }
    }

    fun onClear() {
        searchJob?.cancel()
        savedStateHandle[QUERY_KEY] = ""
        _uiState.value = SearchUiState()
    }

    private companion object {
        const val QUERY_KEY = "query"
    }
}
