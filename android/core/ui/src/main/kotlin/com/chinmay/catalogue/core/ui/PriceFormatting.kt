package com.chinmay.catalogue.core.ui

import com.chinmay.catalogue.core.model.Money
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** "$109.00" in the US, "US$109.00" in India: the currency comes from the data, the format from the device. */
fun Money.formatted(locale: Locale = Locale.getDefault()): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    runCatching { format.currency = Currency.getInstance(currencyCode) }
    return format.format(amount)
}
