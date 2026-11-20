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
}
