package com.chinmay.catalogue.feature.category

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `a product never appears in both top rated and more`() {
        val ssd = TestData.product("10", "SanDisk SSD")
        repository.pages["electronics"] = CategoryPage(TestData.electronics, topRated = listOf(ssd), more = listOf(ssd, TestData.product("11")))

        val state = viewModel("electronics").uiState.value as CategoryUiState.Success

        assertEquals(listOf("10"), state.page.topRated.map { it.id })
        assertEquals(listOf("11"), state.page.more.map { it.id })
    }

    @Test
    fun `an empty category says so instead of showing empty sections`() {
        repository.pages["toys"] = CategoryPage(Category("toys", "Toys"), emptyList(), emptyList())

        val state = viewModel("toys").uiState.value as CategoryUiState.Success

        assertTrue(state.isEmpty)
    }

    @Test
    fun `more products show one page at a time`() {
        val more = (20..34).map { TestData.product(it.toString()) } // 15 products
        repository.pages["electronics"] = CategoryPage(TestData.electronics, topRated = emptyList(), more = more)
        val viewModel = viewModel("electronics")

        assertEquals(6, (viewModel.uiState.value as CategoryUiState.Success).visibleMore.size)
        viewModel.loadMore()
        assertEquals(12, (viewModel.uiState.value as CategoryUiState.Success).visibleMore.size)
        viewModel.loadMore()
        val last = viewModel.uiState.value as CategoryUiState.Success
        assertEquals(15, last.visibleMore.size)
        assertEquals(0, last.remaining)
    }

    @Test
    fun `a category chip switches the page in place and updates the title`() {
        repository.pages["mens-clothing"] = CategoryPage(TestData.mens, listOf(TestData.product("3", "Cotton Jacket", TestData.mens)), emptyList())
        val viewModel = viewModel("electronics")
        assertEquals(listOf(TestData.electronics, TestData.mens), viewModel.categories.value)

        viewModel.selectCategory(TestData.mens)

        assertEquals("Men's clothing", viewModel.category.value.name)
        assertEquals(listOf("3"), (viewModel.uiState.value as CategoryUiState.Success).page.topRated.map { it.id })
    }
}
