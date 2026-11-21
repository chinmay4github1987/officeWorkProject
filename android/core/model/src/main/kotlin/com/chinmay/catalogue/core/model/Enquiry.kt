package com.chinmay.catalogue.core.model

/** A customer's question about one product. Name, email, phone and message are required; company is optional. */
data class EnquiryRequest(
    val productId: String,
    val name: String,
    val email: String,
    val phone: String,
    val message: String,
    val company: String? = null,
)

/** Proof the enquiry reached the sales team; shown on the thank-you screen. */
data class EnquiryReceipt(
    val reference: String,
    val receivedAt: String,
)
