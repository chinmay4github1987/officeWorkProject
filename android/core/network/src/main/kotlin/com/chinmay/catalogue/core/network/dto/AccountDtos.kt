package com.chinmay.catalogue.core.network.dto

import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.CartItem
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.User
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class SessionDto(val token: String, val expiresAt: String, val user: UserDto)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
)

@Serializable
data class CartDto(val items: List<CartItemDto>, val itemCount: Int, val total: MoneyDto)

@Serializable
data class CartItemDto(val product: ProductDto, val quantity: Int, val lineTotal: MoneyDto)

@Serializable
data class AddCartItemDto(val productId: String, val quantity: Int)

@Serializable
data class CartQuantityDto(val quantity: Int)

@Serializable
data class CartEnquiryRequestDto(val phone: String? = null, val message: String? = null)

fun UserDto.asExternalModel() = User(
    id = id,
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
)

fun User.asNetworkModel() = UserDto(id, username, email, firstName, lastName, phone)

fun MoneyDto.asExternalModel() = Money(amount = amount, currencyCode = currency)

fun CartDto.asExternalModel() = Cart(
    items = items.map { CartItem(product = it.product.asExternalModel(), quantity = it.quantity, lineTotal = it.lineTotal.asExternalModel()) },
    itemCount = itemCount,
    total = total.asExternalModel(),
)

fun CartEnquiryRequest.asNetworkModel() = CartEnquiryRequestDto(phone = phone, message = message)
