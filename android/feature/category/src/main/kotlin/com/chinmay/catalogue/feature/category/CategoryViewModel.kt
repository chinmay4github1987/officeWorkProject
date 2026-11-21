package com.chinmay.catalogue.feature.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CategoryPage
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

sealed interface CategoryUiState {
    data object Loading : CategoryUiState
    data class Error(val error: DataError) : CategoryUiState

    /**
     * [page] top-rated first, then the rest; [moreShown] products of the rest are visible ("Load more" adds a page).
     * The two sections never share a product.
     */
    data class Success(val page: CategoryPage, val moreShown: Int = PAGE_SIZE) : CategoryUiState {
        val visibleMore: List<Product> get() = page.more.take(moreShown)
        val remaining: Int get() = (page.more.size - moreShown).coerceAtLeast(0)
        val isEmpty: Boolean get() = page.topRated.isEmpty() && page.more.isEmpty()
        val productCount: Int get() = page.topRated.size + page.more.size
    }

    companion object {
        /** Products added per "Load more". */
        const val PAGE_SIZE = 6
    }
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CatalogueRepository,
) : ViewModel() {

    // Navigation arguments arrive in SavedStateHandle under the route's property names.
    // Reading them by key (rather than toRoute()) keeps this ViewModel testable on the plain JVM.
    private val _category = MutableStateFlow(
        Category(
            slug = checkNotNull(savedStateHandle["slug"]) { "CategoryDestination.slug is required" },
            name = savedStateHandle["name"] ?: "",
        ),
    )

    /** The category on screen. Its name shows in the header straight away, before the page loads. */
    val category: StateFlow<Category> = _category.asStateFlow()

    /** Every category, for the chip row that switches between them. Empty until loaded (the row then hides). */
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    /** Kept for compatibility with screens that show the title before load. */
    val title: String get() = _category.value.name

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            (repository.home() as? DataResult.Success)?.data?.categories?.let { _categories.value = it }
        }
    }

    fun retry() {
        _uiState.value = CategoryUiState.Loading
        load()
    }

    /** A chip in the row: shows that category here, without stacking another screen. */
    fun selectCategory(category: Category) {
        if (category.slug == _category.value.slug) return
        _category.value = category
        _uiState.value = CategoryUiState.Loading
        load()
    }

    fun loadMore() {
        _uiState.update { state ->
            if (state is CategoryUiState.Success && state.remaining > 0) state.copy(moreShown = state.moreShown + CategoryUiState.PAGE_SIZE) else state
        }
    }

    private fun load() {
        loadJob?.cancel() // a newer chip tap wins over an older load still running
        val slug = _category.value.slug
        loadJob = viewModelScope.launch {
            _uiState.value = when (val result = repository.category(slug)) {
                is DataResult.Success -> {
                    // The page's name is canonical (e.g. when opened from a link with no name).
                    _category.update { it.copy(name = result.data.category.name) }
                    CategoryUiState.Success(withoutDuplicates(result.data))
                }
                is DataResult.Failure -> CategoryUiState.Error(result.error)
            }
        }
    }

    /** A product shown in "Top rated" never appears again in "More" (the repository promises this; this enforces it). */
    private fun withoutDuplicates(page: CategoryPage): CategoryPage {
        val topIds = page.topRated.map { it.id }.toSet()
        return page.copy(topRated = page.topRated.distinctBy { it.id }, more = page.more.filter { it.id !in topIds }.distinctBy { it.id })
    }
}
