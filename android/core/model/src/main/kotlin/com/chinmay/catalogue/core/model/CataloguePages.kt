package com.chinmay.catalogue.core.model

/** Everything the home screen needs, in one request. */
data class CatalogueHome(
    val featured: List<Product>,
    val categories: List<Category>,
)

/** A category screen: the "top rated" products first, then the rest of the category. */
data class CategoryPage(
    val category: Category,
    val topRated: List<Product>,
    val more: List<Product>,
)
