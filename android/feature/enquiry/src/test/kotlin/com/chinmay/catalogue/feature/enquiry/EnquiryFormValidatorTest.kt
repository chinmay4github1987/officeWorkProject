package com.chinmay.catalogue.feature.enquiry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnquiryFormValidatorTest {
    private val valid = EnquiryForm(name = "Asha Rao", email = "asha@example.com")

    @Test
    fun `name and email are enough`() {
        assertTrue(EnquiryFormValidator.validate(valid).isEmpty())
    }

    @Test
    fun `name and email are required; whitespace doesn't count`() {
        val errors = EnquiryFormValidator.validate(EnquiryForm(name = "   ", email = ""))
        assertEquals(mapOf(EnquiryField.Name to FieldError.Required, EnquiryField.Email to FieldError.Required), errors)
    }

    @Test
    fun `email must look like an address`() {
        listOf("asha", "asha@", "asha@example", "a sha@example.com").forEach {
            assertEquals(it, FieldError.InvalidEmail, EnquiryFormValidator.validate(valid.copy(email = it))[EnquiryField.Email])
        }
    }

    @Test
    fun `phone is optional, but must be valid when given`() {
        assertTrue(EnquiryFormValidator.validate(valid.copy(phone = "+91 98765 43210")).isEmpty())
        assertEquals(FieldError.InvalidPhone, EnquiryFormValidator.validate(valid.copy(phone = "call me"))[EnquiryField.Phone])
    }

    @Test
    fun `long messages are rejected with the limit`() {
        val errors = EnquiryFormValidator.validate(valid.copy(message = "x".repeat(2001)))
        assertEquals(FieldError.TooLong(2000), errors[EnquiryField.Message])
    }
}
