package com.chinmay.catalogue.core.data

import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.SmtpEnquiryRepository
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.network.mail.SmtpConfig
import com.chinmay.catalogue.core.network.mail.SmtpMailer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

/**
 * Real JavaMail against a tiny local SMTP server (no mocking framework): checks what the sales inbox receives,
 * header-injection safety, and how failures become DataErrors.
 */
class SmtpEnquiryRepositoryTest {

    /** Speaks just enough SMTP for JavaMail, and records each delivered message. */
    private class FakeSmtpServer(private val rejectRecipients: Boolean = false) : AutoCloseable {
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
                    command.startsWith("EHLO") || command.startsWith("HELO") -> reply("250 fake")
                    command.startsWith("MAIL FROM") -> reply("250 OK")
                    command.startsWith("RCPT TO") ->
                        if (rejectRecipients) reply("550 No such user") else reply("250 OK").also { recipients += line.substringAfter(':').trim() }
                    command == "DATA" -> {
                        reply("354 End data with <CR><LF>.<CR><LF>")
                        val data = generateSequence { input.readLine() }.takeWhile { it != "." }.joinToString("\n")
                        deliveries += Delivery(recipients.toList(), data)
                        reply("250 OK queued")
                    }
                    command == "QUIT" -> return reply("221 Bye")
                    else -> reply("250 OK") // RSET, NOOP
                }
            }
        }

        override fun close() = socket.close()
    }

    private class FakeCatalogue(var product: DataResult<Product>) : CatalogueRepository {
        override suspend fun home(): DataResult<CatalogueHome> = error("not used")
        override suspend fun category(slug: String): DataResult<CategoryPage> = error("not used")
        override suspend fun search(term: String): DataResult<List<Product>> = error("not used")
        override suspend fun product(id: String): DataResult<Product> = product
    }

    private lateinit var server: FakeSmtpServer
    private val catalogue = FakeCatalogue(DataResult.Success(BACKPACK))

    @Before
    fun setUp() {
        server = FakeSmtpServer()
    }

    @After
    fun tearDown() = server.close()

    private fun repository(port: Int = server.port) = SmtpEnquiryRepository(
        mailer = SmtpMailer(config(port)),
        catalogue = catalogue,
        clock = { Instant.parse("2026-11-21T09:46:57.321Z") },
    )

    @Test
    fun `sends the enquiry to the sales inbox with the customer in Reply-To`() = runTest {
        val result = repository().submit(REQUEST)

        val receipt = (result as DataResult.Success).data
        assertTrue(receipt.reference.matches(Regex("ENQ-[0-9A-F]{8}")))
        assertEquals("2026-11-21T09:46:57.321Z", receipt.receivedAt)

        val mail = server.deliveries.single()
        assertEquals(listOf("<sales@catalogue.test>"), mail.recipients)
        assertTrue(mail.data.contains("From: no-reply@catalogue.test"))
        assertTrue(mail.data.contains("Reply-To: Asha Patel <asha@example.com>"))
        assertTrue(mail.data.contains("Subject: New enquiry: Fjallraven Backpack"))
        assertTrue(mail.data.contains("Reference: ${receipt.reference}"))
        assertTrue(mail.data.contains("Product: Fjallraven Backpack (ID 1)"))
        assertTrue(mail.data.contains("Price: USD 109.95"))
        assertTrue(mail.data.contains("Phone: +44 7700 900123"))
        assertTrue(mail.data.contains("Is this in stock for 20 units?"))
    }

    @Test
    fun `a line break in the name cannot add headers or recipients`() = runTest {
        repository().submit(REQUEST.copy(name = "Asha\r\nBcc: attacker@evil.test"))

        val mail = server.deliveries.single()
        assertEquals(listOf("<sales@catalogue.test>"), mail.recipients)
        // Headers end at the first blank line. (The name also appears in the body, where it's just text.)
        val headers = mail.data.lines().takeWhile { it.isNotEmpty() }
        assertFalse(headers.any { it.startsWith("Bcc:", ignoreCase = true) })
        assertTrue(headers.any { it.startsWith("Reply-To:") && it.contains("Asha Bcc: attacker@evil.test") })
    }

    @Test
    fun `an invalid email address is a Validation error and nothing is sent`() = runTest {
        val result = repository().submit(REQUEST.copy(email = "not-an-email"))

        assertEquals(DataResult.Failure(DataError.Validation(mapOf("email" to "Enter a valid email address"))), result)
        assertTrue(server.deliveries.isEmpty())
    }

    @Test
    fun `still sends, with the product ID, when product details can't be loaded`() = runTest {
        catalogue.product = DataResult.Failure(DataError.Network)

        val result = repository().submit(REQUEST)

        assertTrue(result is DataResult.Success)
        val data = server.deliveries.single().data
        assertTrue(data.contains("Subject: New enquiry: product 1"))
        assertTrue(data.contains("Product: Product ID 1"))
    }

    @Test
    fun `an unreachable mail server becomes Network`() = runTest {
        val closedPort = ServerSocket(0).use { it.localPort }
        assertEquals(DataResult.Failure(DataError.Network), repository(port = closedPort).submit(REQUEST))
    }

    @Test
    fun `a server that rejects the recipient becomes Server`() = runTest {
        FakeSmtpServer(rejectRecipients = true).use { rejecting ->
            assertEquals(DataResult.Failure(DataError.Server(null)), repository(port = rejecting.port).submit(REQUEST))
        }
    }

    @Test
    fun `the password never appears in the config's text`() {
        assertFalse(config(25).copy(password = "s3cret").toString().contains("s3cret"))
    }

    private companion object {
        fun config(port: Int) = SmtpConfig(
            host = "127.0.0.1",
            port = port,
            security = SmtpConfig.Security.NONE,
            username = "",
            password = "",
            from = "no-reply@catalogue.test",
            salesInbox = "sales@catalogue.test",
            timeoutMillis = 3_000,
        )

        val BACKPACK = Product(
            id = "1",
            title = "Fjallraven Backpack",
            description = "d",
            price = Money(109.95, "USD"),
            category = Category("mens-clothing", "Men's clothing"),
            imageUrl = "https://img.example/1.png",
            rating = Rating(3.9, 120),
        )

        val REQUEST = EnquiryRequest(
            productId = "1",
            name = "Asha Patel",
            email = "asha@example.com",
            phone = "+44 7700 900123",
            message = "Is this in stock for 20 units?",
        )
    }
}
