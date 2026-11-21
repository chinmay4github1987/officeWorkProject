package com.chinmay.catalogue.core.model

/**
 * The app's own product type. Network DTOs are mapped into it, so a data-source change
 * touches one mapper in :core:network, not every screen.
 */
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Money,
    val category: Category,
    val imageUrl: String,
    val rating: Rating,
)

data class Category(
    val slug: String,
    val name: String,
)

data class Money(
    val amount: Double,
    /** ISO 4217, e.g. "USD". Formatting happens in the UI layer, using the device locale. */
    val currencyCode: String,
)

data class Rating(
    val average: Double,
    val count: Int,
)
