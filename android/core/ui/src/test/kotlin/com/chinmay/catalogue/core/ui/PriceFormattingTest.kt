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
}
