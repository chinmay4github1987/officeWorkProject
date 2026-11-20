package com.chinmay.catalogue.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CategoryUiState {
    data object Loading : CategoryUiState
    data class Error(val error: DataError) : CategoryUiState
    data class Success(val page: CategoryPage) : CategoryUiState
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
) : ViewModel() {

    // Navigation arguments arrive in SavedStateHandle under the route's property names.
    // Reading them by key (rather than toRoute()) keeps this ViewModel testable on the plain JVM.
    private val slug: String = checkNotNull(savedStateHandle["slug"]) { "CategoryDestination.slug is required" }

    /** Shown in the top bar straight away, before the page loads. */
    val title: String = savedStateHandle["name"] ?: ""

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = CategoryUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.category(slug)) {
                is DataResult.Success -> CategoryUiState.Success(result.data)
                is DataResult.Failure -> CategoryUiState.Error(result.error)
            }
        }
    }
}
