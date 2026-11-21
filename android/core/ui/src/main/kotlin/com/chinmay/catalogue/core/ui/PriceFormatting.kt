package com.chinmay.catalogue.core.ui

import com.chinmay.catalogue.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * The price as shoppers expect to read it. Rupees always use Indian formatting, "₹9,345.75" and "₹1,23,456.00"
 * (lakh grouping), whatever the phone's language. Other currencies follow the device's locale ("$109.00").
 */
fun Money.formatted(locale: Locale = Locale.getDefault()): String {
    if (currencyCode == "INR") return rupees(amount)
    val format = NumberFormat.getCurrencyInstance(locale)
    runCatching { format.currency = Currency.getInstance(currencyCode) }
    return format.format(amount)
}

/**
 * ₹ with Indian grouping: the last three digits, then groups of two (1,23,45,678.00). Done by hand because
 * java.text only groups in threes on the JVM, while Android's ICU groups by locale: this is the same everywhere.
 */
private fun rupees(amount: Double): String {
    val fixed = BigDecimal.valueOf(amount).abs().setScale(2, RoundingMode.HALF_UP).toPlainString()
    val whole = fixed.substringBefore('.')
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val head = whole.dropLast(3)
        head.reversed().chunked(2).joinToString(",").reversed() + "," + whole.takeLast(3)
    }
    return (if (amount < 0) "-₹" else "₹") + grouped + "." + fixed.substringAfter('.')
}
