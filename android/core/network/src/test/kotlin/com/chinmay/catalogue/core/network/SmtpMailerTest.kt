package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.mail.SmtpConfig
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JavaMail settings for the demo mailbox. Regression: SSL once used "mail.smtps.*", which Transport.send ignores. */
class SmtpMailerTest {
    private val gmail = SmtpConfig(
        host = "smtp.gmail.com",
        port = 465,
        security = SmtpConfig.Security.SSL,
        username = "demo@gmail.com",
        password = "app-password",
        salesInbox = "",
    )

    @Test
    fun `ssl uses the smtp keys, so Transport send really uses the configured server`() {
        val props = SmtpMailer(gmail).properties()
        assertEquals("smtp.gmail.com", props["mail.smtp.host"])
        assertEquals("465", props["mail.smtp.port"])
        assertEquals("true", props["mail.smtp.ssl.enable"])
        assertEquals("true", props["mail.smtp.auth"])
        assertTrue(props.keys.none { it.toString().startsWith("mail.smtps.") })
    }

    @Test
    fun `only used with a username and password, sends to itself without a sales inbox, never logs the password`() {
        assertTrue(gmail.isConfigured)
        assertFalse(gmail.copy(password = "").isConfigured)
        assertEquals("demo@gmail.com", gmail.recipient)
        assertEquals("sales@x.test", gmail.copy(salesInbox = "sales@x.test").recipient)
        assertFalse(gmail.toString().contains("app-password"))
        assertEquals(SmtpConfig.Security.SSL, SmtpConfig.security(""))
        assertEquals(SmtpConfig.Security.STARTTLS, SmtpConfig.security("starttls"))
    }
}
