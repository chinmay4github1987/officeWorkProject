package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.mail.SmtpConfig
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** JavaMail settings per security mode. Regression: SSL settings once used "mail.smtps.*", which Transport.send ignores. */
class SmtpMailerTest {
    private fun config(security: SmtpConfig.Security, port: Int) = SmtpConfig(
        host = "smtp.example.com",
        port = port,
        security = security,
        username = "me@example.com",
        password = "secret",
        from = "",
        salesInbox = "sales@example.com",
    )

    @Test
    fun `ssl uses the smtp keys with ssl enabled, so Transport send really uses the configured server`() {
        val props = SmtpMailer(config(SmtpConfig.Security.SSL, 465)).properties()

        assertEquals("smtp.example.com", props["mail.smtp.host"])
        assertEquals("465", props["mail.smtp.port"])
        assertEquals("true", props["mail.smtp.ssl.enable"])
        assertEquals("true", props["mail.smtp.ssl.checkserveridentity"])
        assertEquals("true", props["mail.smtp.auth"])
        assertTrue(props.keys.none { it.toString().startsWith("mail.smtps.") })
    }

    @Test
    fun `starttls requires the upgrade and doesn't start with ssl`() {
        val props = SmtpMailer(config(SmtpConfig.Security.STARTTLS, 587)).properties()

        assertEquals("true", props["mail.smtp.starttls.enable"])
        assertEquals("true", props["mail.smtp.starttls.required"])
        assertNull(props["mail.smtp.ssl.enable"])
    }

    @Test
    fun `security names from gradle properties`() {
        assertEquals(SmtpConfig.Security.SSL, SmtpConfig.security("ssl"))
        assertEquals(SmtpConfig.Security.SSL, SmtpConfig.security("SMTPS"))
        assertEquals(SmtpConfig.Security.NONE, SmtpConfig.security("none"))
        assertEquals(SmtpConfig.Security.STARTTLS, SmtpConfig.security("starttls"))
        assertEquals(SmtpConfig.Security.STARTTLS, SmtpConfig.security(""))
    }

    @Test
    fun `address check accepts one address and rejects lists and junk`() {
        assertTrue(SmtpMailer.isValidAddress("asha@example.com"))
        assertFalse(SmtpMailer.isValidAddress("not-an-email"))
        assertFalse(SmtpMailer.isValidAddress("a@example.com, b@example.com"))
        assertFalse(SmtpMailer.isValidAddress(""))
    }
}
