package com.chinmay.catalogue.feature.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductUiState {
    data object Loading : ProductUiState
    data class Error(val error: DataError) : ProductUiState
    data class Success(val product: Product) : ProductUiState
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
) : ViewModel() {

    val productId: String = checkNotNull(savedStateHandle["productId"]) { "ProductDestination.productId is required" }

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Loading)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = ProductUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.product(productId)) {
                is DataResult.Success -> ProductUiState.Success(result.data)
                is DataResult.Failure -> ProductUiState.Error(result.error)
            }
        }
    }
}
