package com.chinmay.catalogue.feature.cart

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.FakeCartRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CartViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val carts = FakeCartRepository(auth)
    private fun viewModel() = CartViewModel(auth, carts)
    private fun CartViewModel.ready() = uiState.value as CartUiState.Ready

    @Test
    fun `signed out asks to sign in, then loads the cart once signed in`() = runTest {
        // The cart lives on the server, so it's still there after signing out and back in.
        auth.signIn()
        carts.add("3", 2)
        auth.logout()

        val viewModel = viewModel()
        assertEquals(CartUiState.SignedOut, viewModel.uiState.value)

        auth.signIn()
        assertEquals(2, viewModel.ready().cart.itemCount)
    }

    @Test
    fun `changing a quantity updates the cart and 0 removes the product`() = runTest {
        auth.signIn()
        carts.add("3")
        carts.add("9")
        val viewModel = viewModel()

        viewModel.onQuantityChange("3", 4)
        assertEquals(listOf(4, 1), viewModel.ready().cart.items.map { it.quantity })

        viewModel.onRemove("9")
        assertEquals(listOf("3"), viewModel.ready().cart.items.map { it.product.id })
        assertTrue(viewModel.ready().updating.isEmpty())
    }

    @Test
    fun `a failed update keeps the cart and shows an error`() = runTest {
        auth.signIn()
        carts.add("3")
        val viewModel = viewModel()
        carts.failWith = DataError.Network

        viewModel.onQuantityChange("3", 2)

        assertEquals(1, viewModel.ready().cart.itemCount)
        assertEquals(DataError.Network, viewModel.ready().actionError)
    }

    @Test
    fun `sending the enquiry includes the message and shows the reference`() = runTest {
        auth.signIn()
        carts.add("3")
        val viewModel = viewModel()

        viewModel.onMessageChange("  Is it in stock?  ")
        viewModel.onSendEnquiry()

        val sent = viewModel.uiState.value as CartUiState.Sent
        assertEquals("ENQ-CART0001", sent.receipt.reference)
        assertEquals(TestData.user.email, sent.email)
        assertEquals("Is it in stock?", carts.enquiries.single().message)
    }

    @Test
    fun `signing out elsewhere shows the sign-in prompt`() = runTest {
        auth.signIn()
        val viewModel = viewModel()

        auth.logout()

        assertEquals(CartUiState.SignedOut, viewModel.uiState.value)
    }
}
