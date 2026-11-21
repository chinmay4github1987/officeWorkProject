package com.chinmay.catalogue.core.network.mail

import com.chinmay.catalogue.core.network.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Date
import java.util.Properties
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/** How the app reaches the SMTP server. Built from BuildConfig in NetworkModule. */
data class SmtpConfig(
    val host: String,
    val port: Int,
    val security: Security,
    val username: String,
    val password: String,
    /** The sender address. Falls back to [salesInbox] when blank. */
    val from: String,
    val salesInbox: String,
    val timeoutMillis: Int = 15_000,
) {
    enum class Security { NONE, STARTTLS, SSL }

    /** Email is only sent from the app when a server and an inbox are both configured. */
    val isConfigured: Boolean get() = host.isNotBlank() && salesInbox.isNotBlank()

    // The password must never end up in logs or crash reports.
    override fun toString() = "SmtpConfig(host=$host, port=$port, security=$security, salesInbox=$salesInbox)"

    companion object {
        fun security(value: String): Security = when (value.trim().lowercase()) {
            "none", "plain" -> Security.NONE
            "ssl", "tls", "smtps" -> Security.SSL
            else -> Security.STARTTLS
        }
    }
}

/** One plain-text email to the sales inbox. The customer goes in Reply-To so sales can just press Reply. */
data class OutgoingMail(
    val subject: String,
    val body: String,
    val replyToAddress: String,
    val replyToName: String,
)

/** Sending failed. [isConnectionProblem] = the server couldn't be reached (retrying later may work). */
class MailSendException(val isConnectionProblem: Boolean, cause: Throwable) : IOException(cause.javaClass.simpleName, cause)

/**
 * Sends email over SMTP with JavaMail for Android (com.sun.mail:android-mail).
 * Runs on Dispatchers.IO: JavaMail blocks while it talks to the server.
 */
class SmtpMailer(private val config: SmtpConfig) {

    suspend fun send(mail: OutgoingMail): Unit = withContext(Dispatchers.IO) {
        try {
            val session = Session.getInstance(properties())
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.from.ifBlank { config.salesInbox }))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.salesInbox, true))
                replyTo = arrayOf(InternetAddress(mail.replyToAddress, oneLine(mail.replyToName), "UTF-8"))
                setSubject(oneLine(mail.subject), "UTF-8")
                setText(mail.body, "UTF-8")
                sentDate = Date()
            }
            if (config.username.isBlank()) {
                Transport.send(message)
            } else {
                Transport.send(message, config.username, config.password)
            }
        } catch (e: AuthenticationFailedException) {
            logFailure(e)
            throw MailSendException(isConnectionProblem = false, e)
        } catch (e: MessagingException) {
            logFailure(e)
            // JavaMail wraps socket errors (refused, timeout, unknown host) in a MessagingException.
            throw MailSendException(isConnectionProblem = e.isConnectionProblem(), e)
        }
    }

    /** Debug builds only, and only the exception types: messages can contain addresses, so they're never logged. */
    private fun logFailure(e: MessagingException) {
        if (!BuildConfig.DEBUG) return
        val chain = e.causes().joinToString(" <- ") { it.javaClass.simpleName }
        runCatching { android.util.Log.w("SmtpMailer", "Enquiry email not sent to ${config.host}:${config.port}: $chain") }
    }

    /**
     * Always the "smtp" keys: Transport.send() chooses the transport from the address type (rfc822 -> smtp) and
     * ignores mail.transport.protocol, so "mail.smtps.*" settings would be silently skipped (it would try localhost:25).
     * SSL from the first byte (port 465) is switched on with mail.smtp.ssl.enable instead.
     */
    internal fun properties() = Properties().apply {
        val timeout = config.timeoutMillis.toString()
        put("mail.transport.protocol", "smtp")
        put("mail.smtp.host", config.host)
        put("mail.smtp.port", config.port.toString())
        put("mail.smtp.auth", config.username.isNotBlank().toString())
        put("mail.smtp.connectiontimeout", timeout)
        put("mail.smtp.timeout", timeout)
        put("mail.smtp.writetimeout", timeout)
        when (config.security) {
            SmtpConfig.Security.SSL -> put("mail.smtp.ssl.enable", "true")
            SmtpConfig.Security.STARTTLS -> {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }
            SmtpConfig.Security.NONE -> Unit
        }
        if (config.security != SmtpConfig.Security.NONE) {
            put("mail.smtp.ssl.checkserveridentity", "true")
        }
    }

    private fun MessagingException.causes(): Sequence<Throwable> =
        generateSequence<Throwable>(this) { (it as? MessagingException)?.nextException ?: it.cause }.take(10)

    private fun MessagingException.isConnectionProblem(): Boolean = causes().any { it is IOException }

    companion object {
        /** True for a single, syntactically valid address (what the backend's zod email check accepts, roughly). */
        fun isValidAddress(email: String): Boolean = try {
            InternetAddress(email, true).validate()
            email.contains('@') && !email.contains(',')
        } catch (e: AddressException) {
            false
        }

        /** Headers can't contain line breaks: a name like "x\r\nBcc: ..." must not add a header. */
        private fun oneLine(value: String) = value.replace(Regex("[\\r\\n]+"), " ").trim()
    }
}
