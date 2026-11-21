package com.chinmay.catalogue.feature.product

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.FakeCartRepository
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProductViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCatalogueRepository()
    private val auth = FakeAuthRepository()
    private val carts = FakeCartRepository(auth)

    private fun viewModel(id: String = "3") = ProductViewModel(SavedStateHandle(mapOf("productId" to id)), repository, auth, carts)

    @Test
    fun `loads the product from the navigation argument`() {
        assertEquals("Cotton Jacket", (viewModel().uiState.value as ProductUiState.Success).product.title)
    }

    @Test
    fun `missing product shows NotFound`() {
        assertEquals(ProductUiState.Error(DataError.NotFound), viewModel("999").uiState.value)
    }

    @Test
    fun `adding to cart while signed out asks to sign in and adds nothing`() = runTest {
        val viewModel = viewModel()

        viewModel.addToCart()

        assertEquals(ProductEvent.SignInRequired, viewModel.events.first())
        assertNull(carts.cart.value)
    }

    @Test
    fun `adding to cart while signed in adds one and confirms`() = runTest {
        auth.signIn()
        val viewModel = viewModel()

        viewModel.addToCart()

        assertEquals(ProductEvent.AddedToCart, viewModel.events.first())
        assertEquals(1, carts.cart.value?.itemCount)
        assertEquals(false, (viewModel.uiState.value as ProductUiState.Success).isAddingToCart)
    }

    @Test
    fun `a failed add reports the error`() = runTest {
        auth.signIn()
        carts.failWith = DataError.Network
        val viewModel = viewModel()

        viewModel.addToCart()

        assertEquals(ProductEvent.AddToCartFailed(DataError.Network), viewModel.events.first())
    }

    @Test
    fun `related products come from the same category and never include the product itself`() {
        val state = viewModel("10").uiState.value as ProductUiState.Success

        assertEquals(listOf("9", "11"), state.related.map { it.id })
    }

    @Test
    fun `the description becomes short feature bullets`() {
        assertEquals(
            listOf("Slim and light", "Fits 15 inch laptops", "Water resistant", "two pockets"),
            ProductUiState.featuresOf("Slim and light. Fits 15 inch laptops! Water resistant; two pockets."),
        )
        assertEquals(listOf("One long sentence, with commas, stays whole"), ProductUiState.featuresOf("One long sentence, with commas, stays whole."))
    }

    @Test
    fun `a failed load shows the error and Retry tries again`() {
        repository.failWith = com.chinmay.catalogue.core.model.DataError.Network
        val viewModel = viewModel()
        assertEquals(ProductUiState.Error(com.chinmay.catalogue.core.model.DataError.Network), viewModel.uiState.value)

        repository.failWith = null
        viewModel.retry()
        assertEquals("3", (viewModel.uiState.value as ProductUiState.Success).product.id)
    }
}
