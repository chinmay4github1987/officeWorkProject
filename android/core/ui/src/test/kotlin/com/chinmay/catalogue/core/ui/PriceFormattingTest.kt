package com.chinmay.catalogue.core.ui

import com.chinmay.catalogue.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PriceFormattingTest {
    @Test
    fun `formats with the product currency in the device locale`() {
        assertEquals("$109.00", Money(109.0, "USD").formatted(Locale.US))
        assertEquals("€22.30", Money(22.3, "EUR").formatted(Locale.UK))
    }

    @Test
    fun `rupees use the rupee sign and Indian lakh grouping on any phone`() {
        assertEquals("₹9,345.75", Money(9345.75, "INR").formatted(Locale.US))
        assertEquals("₹1,23,456.00", Money(123456.0, "INR").formatted(Locale.UK))
        assertEquals("₹12,34,56,789.50", Money(123456789.5, "INR").formatted())
        assertEquals("₹999.00", Money(999.0, "INR").formatted())
        assertEquals("₹0.05", Money(0.049, "INR").formatted())
    }
}
