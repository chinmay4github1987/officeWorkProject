package com.chinmay.catalogue.core.model

/** The signed-in customer, as our backend describes them. Never holds a password. */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
) {
    val fullName: String get() = "$firstName $lastName".trim().ifEmpty { username }
}

data class CartItem(
    val product: Product,
    val quantity: Int,
    /** Calculated by the backend from today's price; the app never multiplies prices itself. */
    val lineTotal: Money,
)

data class Cart(
    val items: List<CartItem>,
    /** Sum of quantities: what the cart badge shows. */
    val itemCount: Int,
    val total: Money,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        const val MAX_QUANTITY = 99
    }
}

/** An enquiry about the whole cart. Name and email come from the account, so only extras are sent. */
data class CartEnquiryRequest(
    val phone: String?,
    val message: String?,
)
