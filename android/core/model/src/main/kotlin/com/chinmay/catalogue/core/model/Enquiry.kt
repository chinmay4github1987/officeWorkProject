package com.chinmay.catalogue.core.model

data class EnquiryRequest(
    val productId: String,
    val name: String,
    val email: String,
    val phone: String?,
    val message: String?,
)

/** Proof the enquiry reached the sales team; shown on the thank-you screen. */
data class EnquiryReceipt(
    val reference: String,
    val receivedAt: String,
)
