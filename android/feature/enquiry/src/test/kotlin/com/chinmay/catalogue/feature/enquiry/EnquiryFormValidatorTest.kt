package com.chinmay.catalogue.feature.enquiry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnquiryFormValidatorTest {
    private val valid = EnquiryForm(
        name = "Asha Rao",
        email = "asha@example.com",
        phone = "+91 98765 43210",
        message = "Is this available in large?",
    )

    @Test
    fun `name, email, phone and message are enough, company is optional`() {
        assertTrue(EnquiryFormValidator.validate(valid).isEmpty())
        assertTrue(EnquiryFormValidator.validate(valid.copy(company = "ABC Technologies")).isEmpty())
    }

    @Test
    fun `name, email, phone and message are required, whitespace doesn't count`() {
        val errors = EnquiryFormValidator.validate(EnquiryForm(name = "   ", email = "", phone = " ", message = "  "))
        assertEquals(
            mapOf(
                EnquiryField.Name to FieldError.Required,
                EnquiryField.Email to FieldError.Required,
                EnquiryField.Phone to FieldError.Required,
                EnquiryField.Message to FieldError.Required,
            ),
            errors,
        )
    }

    @Test
    fun `name needs at least two characters`() {
        assertEquals(FieldError.TooShort(2), EnquiryFormValidator.validate(valid.copy(name = " A "))[EnquiryField.Name])
    }

    @Test
    fun `email must look like an address`() {
        listOf("asha", "asha@", "asha@example", "a sha@example.com").forEach {
            assertEquals(it, FieldError.InvalidEmail, EnquiryFormValidator.validate(valid.copy(email = it))[EnquiryField.Email])
        }
    }

    @Test
    fun `phone must be a real number with 7 to 15 digits and optional plus and separators`() {
        listOf("+91 98765 43210", "020 7946 0958", "(555) 123-4567").forEach {
            assertTrue(it, EnquiryFormValidator.validate(valid.copy(phone = it)).isEmpty())
        }
        listOf("call me", "12345", "+1234567890123456", "98765-abc-12").forEach {
            assertEquals(it, FieldError.InvalidPhone, EnquiryFormValidator.validate(valid.copy(phone = it))[EnquiryField.Phone])
        }
    }

    @Test
    fun `message needs a little detail, and has a limit`() {
        assertEquals(FieldError.TooShort(10), EnquiryFormValidator.validate(valid.copy(message = "Hi?"))[EnquiryField.Message])
        assertEquals(FieldError.TooLong(2000), EnquiryFormValidator.validate(valid.copy(message = "x".repeat(2001)))[EnquiryField.Message])
    }

    @Test
    fun `company has a length limit`() {
        assertEquals(FieldError.TooLong(100), EnquiryFormValidator.validate(valid.copy(company = "x".repeat(101)))[EnquiryField.Company])
    }
}
