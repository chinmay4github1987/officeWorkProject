package com.chinmay.catalogue.feature.enquiry

import androidx.lifecycle.SavedStateHandle
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.TestData
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
    private val auth = FakeAuthRepository()
    // Lazy: created inside the test, after MainDispatcherRule has replaced Dispatchers.Main (init launches a coroutine).
    private val viewModel by lazy { EnquiryViewModel(SavedStateHandle(mapOf("productId" to "3")), FakeCatalogueRepository(), enquiries, auth) }

    private fun fill(
        name: String = "  Asha Rao ",
        email: String = "asha@example.com",
        phone: String = " +91 98765 43210 ",
        message: String = "Is it waterproof?",
        company: String = "",
    ) {
        viewModel.onFieldChange(EnquiryField.Name, name)
        viewModel.onFieldChange(EnquiryField.Email, email)
        viewModel.onFieldChange(EnquiryField.Phone, phone)
        viewModel.onFieldChange(EnquiryField.Message, message)
        viewModel.onFieldChange(EnquiryField.Company, company)
    }

    @Test
    fun `shows the product being enquired about`() {
        assertEquals("Cotton Jacket", viewModel.uiState.value.product?.title)
    }

    @Test
    fun `valid form sends a trimmed request and shows the receipt`() {
        fill()
        viewModel.onSubmit()

        assertEquals(listOf(EnquiryRequest("3", "Asha Rao", "asha@example.com", phone = "+91 98765 43210", message = "Is it waterproof?")), enquiries.submitted)
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

    @Test
    fun `a signed-in customer's details are filled in for them`() {
        auth.signIn()

        val form = viewModel.uiState.value.form
        assertEquals(TestData.user.fullName, form.name)
        assertEquals(TestData.user.email, form.email)
        assertEquals(TestData.user.phone, form.phone)
    }

    @Test
    fun `company is sent when given, and left out when blank`() {
        fill(company = "  ABC Technologies ")
        viewModel.onSubmit()

        assertEquals("ABC Technologies", enquiries.submitted.single().company)
    }

    @Test
    fun `missing phone and message are caught before sending`() {
        fill(phone = "", message = "")
        viewModel.onSubmit()

        assertTrue(enquiries.submitted.isEmpty())
        assertEquals(FieldError.Required, viewModel.uiState.value.errors[EnquiryField.Phone])
        assertEquals(FieldError.Required, viewModel.uiState.value.errors[EnquiryField.Message])
    }

    @Test
    fun `no thank-you until the submission is confirmed, and a retry can then succeed`() {
        enquiries.failWith = DataError.Server(null)
        fill()
        viewModel.onSubmit()
        assertNull(viewModel.uiState.value.receipt)
        assertEquals(DataError.Server(null), viewModel.uiState.value.submitError)

        enquiries.failWith = null
        viewModel.onSubmit()
        assertEquals("ENQ-TEST0001", viewModel.uiState.value.receipt?.reference)
    }
}
