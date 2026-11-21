package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.network.mail.MailSendException
import com.chinmay.catalogue.core.network.mail.OutgoingMail
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sends a product enquiry straight to the sales inbox by email (JavaMail over SMTP), instead of through the backend.
 * Used only when the build has SMTP settings; see EnquiryModule. Product details still come from our backend.
 */
internal class SmtpEnquiryRepository(
    private val mailer: SmtpMailer,
    private val catalogue: CatalogueRepository,
    private val clock: () -> Instant,
) : EnquiryRepository {

    @Inject
    constructor(mailer: SmtpMailer, catalogue: CatalogueRepository) : this(mailer, catalogue, Instant::now)

    override suspend fun submit(request: EnquiryRequest): DataResult<EnquiryReceipt> {
        // There's no server-side check on this path, so the email is validated here too.
        if (!SmtpMailer.isValidAddress(request.email)) {
            return DataResult.Failure(DataError.Validation(mapOf("email" to "Enter a valid email address")))
        }
        // The email is more useful with the product's name and price. If they can't be loaded, send it with the ID.
        val product = (catalogue.product(request.productId) as? DataResult.Success)?.data
        val receipt = EnquiryReceipt(
            reference = "ENQ-" + UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT),
            receivedAt = clock().truncatedTo(ChronoUnit.MILLIS).toString(),
        )
        return try {
            mailer.send(EnquiryEmail.compose(request, product, receipt))
            DataResult.Success(receipt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MailSendException) {
            DataResult.Failure(if (e.isConnectionProblem) DataError.Network else DataError.Server(message = null))
        } catch (e: Exception) {
            // Never throw to the UI. No details are logged: the message holds customer data.
            DataResult.Failure(DataError.Server(message = null))
        }
    }
}

/** The email the sales team receives. Same content as the backend's enquiry email. */
internal object EnquiryEmail {
    fun compose(request: EnquiryRequest, product: Product?, receipt: EnquiryReceipt): OutgoingMail {
        val productLine = product?.let { "${it.title} (ID ${it.id})" } ?: "Product ID ${request.productId}"
        val body = buildString {
            appendLine("A customer has sent an enquiry from the product catalogue app.")
            appendLine()
            appendLine("Reference: ${receipt.reference}")
            appendLine("Received: ${receipt.receivedAt}")
            appendLine("Product: $productLine")
            product?.let {
                appendLine("Category: ${it.category.name}")
                appendLine("Price: ${it.price.currencyCode} ${String.format(Locale.ROOT, "%.2f", it.price.amount)}")
            }
            appendLine("Name: ${request.name}")
            appendLine("Email: ${request.email}")
            request.phone?.let { appendLine("Phone: $it") }
            appendLine()
            appendLine("Message:")
            appendLine(request.message ?: "(no message)")
            appendLine()
            append("Reply to this email to answer the customer directly.")
        }
        return OutgoingMail(
            subject = "New enquiry: ${product?.title ?: "product ${request.productId}"}",
            body = body,
            replyToAddress = request.email,
            replyToName = request.name,
        )
    }
}
