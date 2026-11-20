package com.chinmay.catalogue.feature.enquiry

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.FakeEnquiryRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EnquiryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val enquiries = FakeEnquiryRepository()
    // Lazy: created inside the test, after MainDispatcherRule has replaced Dispatchers.Main (init launches a coroutine).
    private val viewModel by lazy { EnquiryViewModel(SavedStateHandle(mapOf("productId" to "3")), FakeCatalogueRepository(), enquiries) }

    private fun fill(name: String = "  Asha Rao ", email: String = "asha@example.com", phone: String = "", message: String = "Is it waterproof?") {
        viewModel.onFieldChange(EnquiryField.Name, name)
        viewModel.onFieldChange(EnquiryField.Email, email)
        viewModel.onFieldChange(EnquiryField.Phone, phone)
        viewModel.onFieldChange(EnquiryField.Message, message)
    }

    @Test
    fun `shows the product being enquired about`() {
        assertEquals("Cotton Jacket", viewModel.uiState.value.product?.title)
    }

    @Test
    fun `valid form sends a trimmed request and shows the receipt`() {
        fill()
        viewModel.onSubmit()

        assertEquals(listOf(EnquiryRequest("3", "Asha Rao", "asha@example.com", phone = null, message = "Is it waterproof?")), enquiries.submitted)
        assertEquals("ENQ-TEST0001", viewModel.uiState.value.receipt?.reference)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `invalid form shows field errors and sends nothing`() {
        fill(name = "", email = "nope")
        viewModel.onSubmit()

        assertTrue(enquiries.submitted.isEmpty())
        assertEquals(setOf(EnquiryField.Name, EnquiryField.Email), viewModel.uiState.value.errors.keys)
    }

    @Test
    fun `editing a field clears its error`() {
        fill(name = "")
        viewModel.onSubmit()
        viewModel.onFieldChange(EnquiryField.Name, "Asha")

        assertNull(viewModel.uiState.value.errors[EnquiryField.Name])
    }

    @Test
    fun `server validation errors land on the matching field`() {
        enquiries.failWith = DataError.Validation(mapOf("email" to "That email domain does not exist"))
        fill()
        viewModel.onSubmit()

        assertEquals(FieldError.Server("That email domain does not exist"), viewModel.uiState.value.errors[EnquiryField.Email])
        assertNull(viewModel.uiState.value.receipt)
    }

    @Test
    fun `offline keeps the form filled in and shows a banner`() {
        enquiries.failWith = DataError.Network
        fill()
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(DataError.Network, state.submitError)
        assertEquals("  Asha Rao ", state.form.name)
        assertNull(state.receipt)
    }

    @Test
    fun `cannot submit twice once it has been sent`() {
        fill()
        viewModel.onSubmit()
        viewModel.onSubmit()

        assertEquals(1, enquiries.submitted.size)
    }
}
