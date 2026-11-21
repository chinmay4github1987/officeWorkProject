package com.chinmay.catalogue.core.network.dto

import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.model.User
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Base64
import java.util.Locale

/*
 * Fake Store's JSON, exactly as it comes (https://fakestoreapi.com/docs), and its mapping to :core:model.
 * Fields the app doesn't need are simply not declared: Json(ignoreUnknownKeys) skips them. That's how the
 * password and home address Fake Store returns in /users never get parsed, kept or logged.
 */

/**
 * How Fake Store's prices (always US dollars) are shown: converted to [currencyCode] at [perUsd] units per dollar,
 * rounded to the paisa/cent. The app shows rupees by default (see core/network/build.gradle.kts). The rate is a
 * fixed setting, not a live exchange rate: Fake Store's prices are demo data, so a reference rate is enough.
 */
data class Pricing(val currencyCode: String, val perUsd: Double) {
    init {
        require(perUsd > 0) { "perUsd must be positive" }
    }

    fun fromUsd(usd: Double): Money =
        Money(BigDecimal.valueOf(usd).multiply(BigDecimal.valueOf(perUsd)).setScale(2, RoundingMode.HALF_UP).toDouble(), currencyCode)

    companion object {
        /** Fake Store's own prices, unconverted. */
        val USD = Pricing("USD", 1.0)
    }
}

@Serializable
data class FakeStoreProductDto(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String = "",
    val category: String,
    val image: String,
    val rating: FakeStoreRatingDto = FakeStoreRatingDto(),
)

@Serializable
data class FakeStoreRatingDto(val rate: Double = 0.0, val count: Int = 0)

@Serializable
data class FakeStoreLoginRequestDto(val username: String, val password: String)

@Serializable
data class FakeStoreTokenDto(val token: String)

@Serializable
data class FakeStoreUserDto(
    val id: Int,
    val email: String,
    val username: String,
    val name: FakeStoreNameDto = FakeStoreNameDto(),
    val phone: String? = null,
)

@Serializable
data class FakeStoreNameDto(val firstname: String = "", val lastname: String = "")

@Serializable
data class FakeStoreCartDto(
    val id: Int,
    val userId: Int,
    val date: String = "",
    val products: List<FakeStoreCartLineDto> = emptyList(),
)

@Serializable
data class FakeStoreCartLineDto(val productId: Int, val quantity: Int)

fun FakeStoreProductDto.asExternalModel(pricing: Pricing = Pricing.USD) = Product(
    id = id.toString(),
    title = title.trim(),
    description = description.trim(),
    price = pricing.fromUsd(price),
    category = fakeStoreCategory(category),
    imageUrl = image,
    rating = Rating(average = rating.rate, count = rating.count),
)

/** Fake Store stores names in lower case ("john"), so they're capitalised for display ("John"). */
fun FakeStoreUserDto.asExternalModel() = User(
    id = id.toString(),
    username = username,
    email = email,
    firstName = name.firstname.trim().replaceFirstChar { it.titlecase(Locale.ROOT) },
    lastName = name.lastname.trim().replaceFirstChar { it.titlecase(Locale.ROOT) },
    phone = phone?.takeIf { it.isNotBlank() },
)

/** "men's clothing" -> Category(slug = "mens-clothing", name = "Men's clothing"). Same rule the backend used. */
fun fakeStoreCategory(raw: String): Category {
    val name = raw.trim()
    return Category(slug = slugify(name), name = name.replaceFirstChar { it.titlecase(Locale.ROOT) })
}

private fun slugify(name: String): String =
    Normalizer.normalize(name, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "") // accents
        .lowercase(Locale.ROOT)
        .replace(Regex("['’]"), "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

/**
 * The user ID inside Fake Store's login token (a JWT: header.payload.signature, payload {"sub": 1, ...}).
 * Read without verifying the signature: the app can't (it doesn't know Fake Store's key), and it only uses the ID to
 * load that user's public profile and cart from Fake Store itself. Null if the token isn't shaped as expected.
 */
fun fakeStoreUserIdFromToken(token: String, json: Json): String? = runCatching {
    val payload = token.split('.')[1]
    val decoded = String(Base64.getUrlDecoder().decode(payload.padEnd((payload.length + 3) / 4 * 4, '=')), Charsets.UTF_8)
    val sub = json.parseToJsonElement(decoded).jsonObject["sub"]?.jsonPrimitive ?: return null
    (sub.intOrNull ?: sub.content.toInt()).toString()
}.getOrNull()

/** Parses GET /products/{id}: null for Fake Store's empty "not found" body. */
fun parseFakeStoreProduct(body: String, json: Json, pricing: Pricing = Pricing.USD): Product? =
    body.trim().takeIf { it.isNotEmpty() && it != "null" }
        ?.let { json.decodeFromString(FakeStoreProductDto.serializer(), it).asExternalModel(pricing) }
