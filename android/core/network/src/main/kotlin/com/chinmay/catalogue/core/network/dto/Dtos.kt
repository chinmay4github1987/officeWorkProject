package com.chinmay.catalogue.core.network.dto

import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import kotlinx.serialization.Serializable

/*
 * DTOs mirror the JSON exactly; asExternalModel() turns them into :core:model types.
 * Keeping the two apart means a renamed JSON field is fixed here, not in every screen.
 */

@Serializable
data class ProductDto(
    val id: String,
    val title: String,
    val description: String,
    val price: MoneyDto,
    val category: CategoryDto,
    val imageUrl: String,
    val rating: RatingDto,
)

@Serializable
data class MoneyDto(val amount: Double, val currency: String)

@Serializable
data class RatingDto(val average: Double, val count: Int)

@Serializable
data class CategoryDto(val slug: String, val name: String)

@Serializable
data class HomeDto(val featured: List<ProductDto>, val categories: List<CategoryDto>)

@Serializable
data class CategoryPageDto(val category: CategoryDto, val topRated: List<ProductDto>, val more: List<ProductDto>)

@Serializable
data class ProductListDto(val query: String? = null, val products: List<ProductDto>)

@Serializable
data class EnquiryRequestDto(
    val productId: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val message: String? = null,
)

@Serializable
data class EnquiryReceiptDto(val reference: String, val receivedAt: String)

/** Every backend error has this shape: { "error": { "code", "message", "fields"? } } */
@Serializable
data class ErrorEnvelopeDto(val error: ErrorDto)

@Serializable
data class ErrorDto(val code: String, val message: String, val fields: Map<String, String> = emptyMap())

fun ProductDto.asExternalModel() = Product(
    id = id,
    title = title,
    description = description,
    price = Money(amount = price.amount, currencyCode = price.currency),
    category = category.asExternalModel(),
    imageUrl = imageUrl,
    rating = Rating(average = rating.average, count = rating.count),
)

fun CategoryDto.asExternalModel() = Category(slug = slug, name = name)

fun HomeDto.asExternalModel() = CatalogueHome(
    featured = featured.map { it.asExternalModel() },
    categories = categories.map { it.asExternalModel() },
)

fun CategoryPageDto.asExternalModel() = CategoryPage(
    category = category.asExternalModel(),
    topRated = topRated.map { it.asExternalModel() },
    more = more.map { it.asExternalModel() },
)

fun EnquiryReceiptDto.asExternalModel() = EnquiryReceipt(reference = reference, receivedAt = receivedAt)

fun EnquiryRequest.asNetworkModel() = EnquiryRequestDto(
    productId = productId,
    name = name,
    email = email,
    phone = phone,
    message = message,
)
