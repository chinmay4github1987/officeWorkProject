package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.dto.HomeDto
import com.chinmay.catalogue.core.network.dto.asExternalModel
import org.junit.Assert.assertEquals
import org.junit.Test

class DtoMappingTest {
    @Test
    fun `parses the backend home response and maps it to the domain model`() {
        val body = """
            {
              "featured": [{
                "id": "3", "title": "Cotton Jacket", "description": "Warm.",
                "price": { "amount": 55.99, "currency": "USD" },
                "category": { "slug": "mens-clothing", "name": "Men's clothing" },
                "imageUrl": "https://img.example/3.png",
                "rating": { "average": 4.7, "count": 500 },
                "addedInV2": "ignored by older apps"
              }],
              "categories": [{ "slug": "mens-clothing", "name": "Men's clothing" }]
            }
        """.trimIndent()

        val home = NetworkFactory.json.decodeFromString<HomeDto>(body).asExternalModel()

        val product = home.featured.single()
        assertEquals("3", product.id)
        assertEquals(55.99, product.price.amount, 0.0)
        assertEquals("USD", product.price.currencyCode)
        assertEquals("mens-clothing", product.category.slug)
        assertEquals(4.7, product.rating.average, 0.0)
        assertEquals("Men's clothing", home.categories.single().name)
    }
}
