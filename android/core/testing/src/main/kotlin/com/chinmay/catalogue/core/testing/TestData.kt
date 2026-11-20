package com.chinmay.catalogue.core.testing

import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating

object TestData {
    val electronics = Category("electronics", "Electronics")
    val mens = Category("mens-clothing", "Men's clothing")

    fun product(id: String, title: String = "Product $id", category: Category = electronics) = Product(
        id = id,
        title = title,
        description = "$title description",
        price = Money(amount = id.toDouble() * 10, currencyCode = "USD"),
        category = category,
        imageUrl = "https://img.example/$id.png",
        rating = Rating(average = 4.0, count = 100),
    )

    val products = listOf(
        product("1", "Fjallraven Backpack", mens),
        product("3", "Cotton Jacket", mens),
        product("9", "Portable Hard Drive"),
        product("10", "SanDisk SSD"),
        product("11", "Silicon Power SSD"),
    )

    val home = CatalogueHome(featured = products.take(5), categories = listOf(electronics, mens))

    val electronicsPage = CategoryPage(
        category = electronics,
        topRated = products.filter { it.category == electronics }.take(2),
        more = products.filter { it.category == electronics }.drop(2),
    )
}
