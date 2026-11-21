package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.SmtpEnquirySender
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.network.mail.SmtpConfig
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/** The optional demo-mailbox route: real JavaMail against a tiny local SMTP server (no mocking framework). */
class SmtpEnquirySenderTest {

    /** Speaks just enough SMTP (with AUTH) for JavaMail, and records each delivered message. */
    private class FakeSmtpServer : AutoCloseable {
        class Delivery(val recipients: List<String>, val data: String)

        private val socket = ServerSocket(0)
        val port: Int get() = socket.localPort
        val deliveries = CopyOnWriteArrayList<Delivery>()

        init {
            thread(isDaemon = true) {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    client.use { serve(BufferedReader(InputStreamReader(it.getInputStream(), Charsets.UTF_8)), PrintWriter(it.getOutputStream(), true)) }
                }
            }
        }

        private fun serve(input: BufferedReader, output: PrintWriter) {
            fun reply(line: String) = output.print("$line\r\n").also { output.flush() }
            val recipients = mutableListOf<String>()
            reply("220 fake ESMTP")
            while (true) {
                val line = input.readLine() ?: return
                val command = line.uppercase()
                when {
                    command.startsWith("EHLO") -> { output.print("250-fake\r\n250 AUTH PLAIN LOGIN\r\n"); output.flush() }
                    command.startsWith("AUTH") -> reply("235 Authentication successful")
                    command.startsWith("MAIL FROM") -> reply("250 OK")
                    command.startsWith("RCPT TO") -> reply("250 OK").also { recipients += line.substringAfter(':').trim() }
                    command == "DATA" -> {
                        reply("354 End data with <CR><LF>.<CR><LF>")
                        val data = generateSequence { input.readLine() }.takeWhile { it != "." }.joinToString("\n")
                        deliveries += Delivery(recipients.toList(), data)
                        reply("250 OK queued")
                    }
                    command == "QUIT" -> return reply("221 Bye")
                    else -> reply("250 OK")
                }
            }
        }

        override fun close() = socket.close()
    }

    private val server = FakeSmtpServer()

    @After
    fun tearDown() = server.close()

    private fun config(port: Int) = SmtpConfig(
        host = "127.0.0.1",
        port = port,
        security = SmtpConfig.Security.NONE,
        username = "demo@example.test",
        password = "app-password",
        salesInbox = "",
        timeoutMillis = 3_000,
    )

    private val fields = mapOf(
        "_subject" to "New Product Enquiry – Mens Cotton Jacket",
        "reference" to "ENQ-ABCD1234",
        "productId" to "3",
        "productName" to "Mens Cotton Jacket",
        "name" to "Asha Patel",
        "email" to "asha@example.com",
        "phone" to "+91 98765 43210",
        "company" to "",
        "message" to "Is this available in large?",
    )

    @Test
    fun `emails the enquiry to the demo mailbox with the customer as reply-to`() = runTest {
        val result = SmtpEnquirySender(SmtpMailer(config(server.port))).submit(fields)

        assertEquals(DataResult.Success(Unit), result)
        val mail = server.deliveries.single()
        assertEquals(listOf("<demo@example.test>"), mail.recipients) // no SALES_INBOX: sent to the mailbox itself
        val headers = mail.data.lines().takeWhile { it.isNotEmpty() }
        assertTrue(headers.any { it.startsWith("Reply-To: Asha Patel <asha@example.com>") })
        assertTrue(headers.any { it.startsWith("Subject:") })
        val body = SmtpEnquirySender.mail(fields).body
        assertTrue(body.contains("Product name: Mens Cotton Jacket"))
        assertTrue(body.contains("Customer phone: +91 98765 43210"))
        assertTrue(body.contains("Message:\nIs this available in large?"))
        assertFalse(body.contains("Company")) // blank fields are left out
    }

    @Test
    fun `a line break in the customer's name can't add a header`() = runTest {
        SmtpEnquirySender(SmtpMailer(config(server.port))).submit(fields + ("name" to "Asha\r\nBcc: attacker@evil.test"))

        val headers = server.deliveries.single().data.lines().takeWhile { it.isNotEmpty() }
        assertFalse(headers.any { it.startsWith("Bcc:", ignoreCase = true) })
    }

    @Test
    fun `an unreachable mail server becomes Network`() = runTest {
        val closedPort = ServerSocket(0).use { it.localPort }
        assertEquals(DataResult.Failure(DataError.Network), SmtpEnquirySender(SmtpMailer(config(closedPort))).submit(fields))
    }

}
