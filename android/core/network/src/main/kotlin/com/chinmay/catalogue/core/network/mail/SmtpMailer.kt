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
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * The optional demo mailbox (e.g. Gmail) the app can send enquiries through. Built from BuildConfig in NetworkModule.
 * Used only when [isConfigured]; otherwise enquiries go to Formspree.
 */
data class SmtpConfig(
    val host: String,
    val port: Int,
    val security: Security,
    val username: String,
    val password: String,
    /** Where enquiries go. Defaults to the sending mailbox itself. */
    val salesInbox: String,
    val timeoutMillis: Int = 15_000,
) {
    enum class Security { NONE, STARTTLS, SSL }

    val isConfigured: Boolean get() = host.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    val recipient: String get() = salesInbox.ifBlank { username }

    // The password must never end up in logs or crash reports.
    override fun toString() = "SmtpConfig(host=$host, port=$port, security=$security, username=$username)"

    companion object {
        fun security(value: String): Security = when (value.trim().lowercase()) {
            "none", "plain" -> Security.NONE
            "starttls", "tls" -> Security.STARTTLS
            else -> Security.SSL
        }
    }
}

/** One plain-text email. The customer goes in Reply-To so the sales team can just press Reply. */
data class OutgoingMail(
    val subject: String,
    val body: String,
    val replyToAddress: String,
    val replyToName: String,
)

/**
 * Sending failed. [isConnectionProblem] = the server couldn't be reached (retrying later may work);
 * [isLoginRejected] = the mailbox refused the username/password (for Gmail: an App Password is required).
 */
class MailSendException(
    val isConnectionProblem: Boolean,
    cause: Throwable,
    val isLoginRejected: Boolean = false,
) : IOException(cause.javaClass.simpleName, cause)

/**
 * Sends email over SMTP with JavaMail for Android (com.sun.mail:android-mail), from and to the configured mailbox.
 * Runs on Dispatchers.IO: JavaMail blocks while it talks to the server.
 */
class SmtpMailer(private val config: SmtpConfig) {

    suspend fun send(mail: OutgoingMail): Unit = withContext(Dispatchers.IO) {
        try {
            val message = MimeMessage(Session.getInstance(properties())).apply {
                setFrom(InternetAddress(config.username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.recipient, true))
                replyTo = arrayOf(InternetAddress(mail.replyToAddress, oneLine(mail.replyToName), "UTF-8"))
                setSubject(oneLine(mail.subject), "UTF-8")
                setText(mail.body, "UTF-8")
                sentDate = Date()
            }
            Transport.send(message, config.username, config.password)
        } catch (e: AuthenticationFailedException) {
            logFailure(e)
            throw MailSendException(isConnectionProblem = false, e, isLoginRejected = true)
        } catch (e: MessagingException) {
            logFailure(e)
            // JavaMail wraps socket errors (refused, timeout, unknown host, TLS) in a MessagingException.
            throw MailSendException(isConnectionProblem = e.causes().any { it is IOException }, e)
        }
    }

    /**
     * Always the "smtp" keys: Transport.send() picks the transport from the address type (rfc822 -> smtp) and
     * ignores mail.transport.protocol, so "mail.smtps.*" settings would be skipped. SSL is mail.smtp.ssl.enable.
     */
    internal fun properties() = Properties().apply {
        val timeout = config.timeoutMillis.toString()
        put("mail.transport.protocol", "smtp")
        put("mail.smtp.host", config.host)
        put("mail.smtp.port", config.port.toString())
        put("mail.smtp.auth", "true")
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
        if (config.security != SmtpConfig.Security.NONE) put("mail.smtp.ssl.checkserveridentity", "true")
    }

    /** Debug builds only, and only the exception types: messages can contain addresses, so they're never logged. */
    private fun logFailure(e: MessagingException) {
        if (!BuildConfig.DEBUG) return
        val chain = e.causes().joinToString(" <- ") { it.javaClass.simpleName }
        runCatching { android.util.Log.w("SmtpMailer", "Enquiry email not sent via ${config.host}:${config.port}: $chain") }
    }

    private fun MessagingException.causes(): Sequence<Throwable> =
        generateSequence<Throwable>(this) { (it as? MessagingException)?.nextException ?: it.cause }.take(10)

    private companion object {
        /** Headers can't contain line breaks: a name like "x\r\nBcc: ..." must not add a header. */
        fun oneLine(value: String) = value.replace(Regex("[\\r\\n]+"), " ").trim()
    }
}
