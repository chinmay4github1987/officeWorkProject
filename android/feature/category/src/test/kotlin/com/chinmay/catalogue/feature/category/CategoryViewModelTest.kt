package com.chinmay.catalogue.feature.category

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CategoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()

    private fun viewModel(slug: String) =
        CategoryViewModel(SavedStateHandle(mapOf("slug" to slug, "name" to "Electronics")), repository)

    @Test
    fun `shows top rated products first, then the rest of the category`() {
        val state = viewModel("electronics").uiState.value as CategoryUiState.Success

        assertEquals(TestData.electronicsPage.topRated, state.page.topRated)
        assertEquals(TestData.electronicsPage.more, state.page.more)
    }

    @Test
    fun `uses the name from navigation as the title before loading`() {
        assertEquals("Electronics", viewModel("electronics").title)
    }

    @Test
    fun `unknown category shows NotFound`() {
        assertEquals(CategoryUiState.Error(DataError.NotFound), viewModel("garden").uiState.value)
    }
}
