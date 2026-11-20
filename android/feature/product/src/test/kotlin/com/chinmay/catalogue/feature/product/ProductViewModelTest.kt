package com.chinmay.catalogue.feature.product

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()

    @Test
    fun `loads the product from the navigation argument`() {
        val viewModel = ProductViewModel(SavedStateHandle(mapOf("productId" to "3")), repository)
        assertEquals("Cotton Jacket", (viewModel.uiState.value as ProductUiState.Success).product.title)
    }

    @Test
    fun `missing product shows NotFound`() {
        val viewModel = ProductViewModel(SavedStateHandle(mapOf("productId" to "999")), repository)
        assertEquals(ProductUiState.Error(DataError.NotFound), viewModel.uiState.value)
    }
}
