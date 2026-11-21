package com.chinmay.catalogue.core.data.repository

import android.util.Log
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.network.mail.MailSendException
import com.chinmay.catalogue.core.network.mail.OutgoingMail
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Delivers an enquiry's fields to the sales team and says whether that was confirmed. "_subject" is the email
 * subject; "email" and "name" are the customer (reply-to). Three ways: Formspree ([FormspreeClient], when configured),
 * a demo mailbox over SMTP ([SmtpEnquirySender], when SMTP is configured), or local demo mode ([DemoEnquirySender]).
 */
/** Shown when the demo mailbox refuses the app's login (for Gmail: use an App Password, not the account password). */
internal const val EMAIL_LOGIN_REJECTED =
    "The app's email account couldn't sign in, so your enquiry wasn't sent. Please try again later."

internal interface EnquirySender {
    suspend fun submit(fields: Map<String, String>): DataResult<Unit>
}

/**
 * Local demo enquiry sender used when neither SMTP nor Formspree is configured.
 * Simulates network transmission with a short delay and confirms the enquiry
 * so reviewers, testers, and demo users can test the enquiry flow smoothly.
 */
internal class DemoEnquirySender @Inject constructor() : EnquirySender {
    override suspend fun submit(fields: Map<String, String>): DataResult<Unit> {
        delay(400)
        Log.i("DemoEnquirySender", "Enquiry submitted in demo mode: ${fields["_subject"]} (ref: ${fields["reference"]})")
        return DataResult.Success(Unit)
    }
}

/**
 * Emails the enquiry from the demo mailbox (e.g. Gmail) to the sales inbox, straight from the app. For client demos
 * only: the mailbox password is inside the APK.
 */
internal class SmtpEnquirySender @Inject constructor(private val mailer: SmtpMailer) : EnquirySender {

    override suspend fun submit(fields: Map<String, String>): DataResult<Unit> = try {
        mailer.send(mail(fields))
        DataResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: MailSendException) {
        DataResult.Failure(
            when {
                e.isConnectionProblem -> DataError.Network
                // Shown on the form, so a demo with a wrong mailbox password is obvious instead of "Something went wrong".
                e.isLoginRejected -> DataError.Server(EMAIL_LOGIN_REJECTED)
                else -> DataError.Server(message = null)
            },
        )
    }

    internal companion object {
        /** Field labels for the email body, in reading order; anything else follows with its own name. */
        private val LABELS = linkedMapOf(
            "reference" to "Reference",
            "submittedAt" to "Submitted",
            "name" to "Customer name",
            "email" to "Customer email",
            "phone" to "Customer phone",
            "company" to "Company",
            "productId" to "Product ID",
            "productName" to "Product name",
            "productCategory" to "Category",
            "productUrl" to "Product link",
            "itemCount" to "Items",
            "items" to "Cart",
            "total" to "Total",
            "message" to "Message",
        )

        fun mail(fields: Map<String, String>): OutgoingMail {
            val shown = fields.filterKeys { !it.startsWith("_") }.filterValues { it.isNotBlank() }
            val ordered = LABELS.keys.filter { it in shown } + shown.keys.filter { it !in LABELS }
            val body = buildString {
                appendLine("A customer has sent an enquiry from the product catalogue app.")
                appendLine()
                ordered.forEach { key ->
                    val value = shown.getValue(key)
                    val label = LABELS[key] ?: key
                    if ('\n' in value || key == "message") {
                        appendLine("$label:")
                        appendLine(value)
                    } else {
                        appendLine("$label: $value")
                    }
                }
                appendLine()
                append("Reply to this email to answer the customer directly.")
            }
            return OutgoingMail(
                subject = fields["_subject"] ?: "New enquiry",
                body = body,
                replyToAddress = fields["email"].orEmpty(),
                replyToName = fields["name"].orEmpty(),
            )
        }
    }
}
