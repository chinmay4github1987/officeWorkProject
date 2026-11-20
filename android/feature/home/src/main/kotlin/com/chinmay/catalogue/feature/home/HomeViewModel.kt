package com.chinmay.catalogue.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Each screen state is one of these; the UI renders exactly one, so impossible combinations can't happen. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val error: DataError) : HomeUiState
    data class Success(val home: CatalogueHome, val isRefreshing: Boolean = false) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CatalogueRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _uiState.value = HomeUiState.Loading
        load()
    }

    /** Pull to refresh: keeps showing the current products while it reloads, and keeps them if it fails. */
    fun refresh() {
        val current = _uiState.value as? HomeUiState.Success ?: return retry()
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            when (val result = repository.home()) {
                is DataResult.Success -> _uiState.value = HomeUiState.Success(result.data)
                is DataResult.Failure -> _uiState.update { (it as? HomeUiState.Success)?.copy(isRefreshing = false) ?: it }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = when (val result = repository.home()) {
                is DataResult.Success -> HomeUiState.Success(result.data)
                is DataResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }
}
