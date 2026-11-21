package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.httpError
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.network.FormspreeApi
import com.chinmay.catalogue.core.network.FormspreeConfig
import com.chinmay.catalogue.core.network.dto.parseFormspreeErrors
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Shown when this build has no Formspree form: there is nowhere to send an enquiry. */
internal const val ENQUIRIES_NOT_SET_UP = "Enquiries can't be sent from this version of the app yet. Please try again later."

/** Where a product's details live, so sales can open it: Fake Store's own product URL. */
internal fun productUrl(productId: String) = "https://fakestoreapi.com/products/$productId"

/**
 * Posts a submission to the configured Formspree form and says whether Formspree confirmed it.
 * Success only on a 2xx with {"ok": true}; Formspree's 422 field errors become DataError.Validation.
 */
internal class FormspreeClient @Inject constructor(
    private val api: FormspreeApi,
    private val config: FormspreeConfig,
    private val json: Json,
) : EnquirySender {
    override suspend fun submit(fields: Map<String, String>): DataResult<Unit> {
        if (!config.isConfigured) return DataResult.Failure(DataError.Server(ENQUIRIES_NOT_SET_UP))
        return try {
            if (api.submit(config.formId, fields).ok) DataResult.Success(Unit) else DataResult.Failure(DataError.Server(message = null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            DataResult.Failure(if (e.code() == 422) rejected(e) else httpError(e.code()))
        } catch (e: IOException) {
            DataResult.Failure(DataError.Network)
        } catch (e: SerializationException) {
            DataResult.Failure(DataError.Server(message = null))
        }
    }

    private fun rejected(e: HttpException): DataError {
        val body = parseFormspreeErrors(e.response()?.errorBody()?.string(), json)
        val fields = body?.errors.orEmpty()
            .mapNotNull { error -> error.field?.takeIf { it.isNotBlank() }?.let { it to error.message } }
            .toMap()
        return if (fields.isNotEmpty()) {
            DataError.Validation(fields)
        } else {
            DataError.Server(body?.errors?.firstOrNull()?.message ?: body?.error)
        }
    }
}

/** A new enquiry reference and timestamp, the same for product and cart enquiries. */
internal fun newReceipt(clock: () -> Instant) = EnquiryReceipt(
    reference = "ENQ-" + UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT),
    receivedAt = clock().truncatedTo(ChronoUnit.MILLIS).toString(),
)

/**
 * Product enquiries go to the [EnquirySender]: Formspree (stores them and emails the sales inbox) or the demo Gmail. Product details (name, category)
 * come from the Fake Store catalogue; if they can't be loaded, the enquiry still goes with the product ID.
 */
internal class FormspreeEnquiryRepository(
    private val sender: EnquirySender,
    private val catalogue: CatalogueRepository,
    private val clock: () -> Instant,
) : EnquiryRepository {

    @Inject
    constructor(sender: EnquirySender, catalogue: CatalogueRepository) : this(sender, catalogue, Instant::now)

    override suspend fun submit(request: EnquiryRequest): DataResult<EnquiryReceipt> {
        val product = (catalogue.product(request.productId) as? DataResult.Success)?.data
        val receipt = newReceipt(clock)
        val productName = product?.title ?: "Product ${request.productId}"
        val fields = buildMap {
            // Formspree uses "_subject" as the email subject and "email" as the reply-to address.
            put("_subject", "New Product Enquiry – $productName")
            put("reference", receipt.reference)
            put("submittedAt", receipt.receivedAt)
            put("productId", request.productId)
            put("productName", productName)
            put("productCategory", product?.category?.name ?: "")
            put("productUrl", productUrl(request.productId))
            put("name", request.name)
            put("email", request.email)
            put("phone", request.phone)
            put("company", request.company.orEmpty())
            put("message", request.message)
        }
        return when (val result = sender.submit(fields)) {
            is DataResult.Success -> DataResult.Success(receipt)
            is DataResult.Failure -> result
        }
    }
}
