package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.data.session.CartLineStore
import com.chinmay.catalogue.core.data.session.SessionStore
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartEnquiryRequest
import com.chinmay.catalogue.core.model.CartItem
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.User
import com.chinmay.catalogue.core.network.FakeStoreApi
import com.chinmay.catalogue.core.network.dto.Pricing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

/**
 * The signed-in customer's cart. Every call needs a session; signed out, calls fail with DataError.Unauthorized.
 * Every successful call updates [cart], so badges and screens stay in step.
 */
interface CartRepository {
    /** The latest cart, or null before the first load (or when signed out). Pair with AuthRepository.currentUser. */
    val cart: StateFlow<Cart?>

    suspend fun refresh(): DataResult<Cart>
    suspend fun add(productId: String, quantity: Int = 1): DataResult<Cart>

    /** 0 removes the product. */
    suspend fun setQuantity(productId: String, quantity: Int): DataResult<Cart>
    suspend fun remove(productId: String): DataResult<Cart>

    /** Emails the sales team about the whole cart, then empties it. */
    suspend fun submitEnquiry(request: CartEnquiryRequest): DataResult<EnquiryReceipt>
}

/**
 * The cart lives on the phone (Fake Store never saves cart changes). The first time a customer opens it here, their
 * newest Fake Store cart (GET /carts/user/{id}) becomes the starting cart. Prices always come from the live catalogue,
 * never from storage, and totals are rounded to the cent. Enquiries go to Formspree, which emails the sales inbox.
 */
internal class LocalCartRepository(
    private val api: FakeStoreApi,
    private val catalogue: CatalogueRepository,
    private val session: SessionStore,
    private val store: CartLineStore,
    private val sender: EnquirySender,
    private val pricing: Pricing,
    private val clock: () -> Instant,
) : CartRepository {

    @Inject
    constructor(
        api: FakeStoreApi,
        catalogue: CatalogueRepository,
        session: SessionStore,
        store: CartLineStore,
        sender: EnquirySender,
        pricing: Pricing,
    ) : this(api, catalogue, session, store, sender, pricing, Instant::now)

    private val _cart = MutableStateFlow<Cart?>(null)
    override val cart: StateFlow<Cart?> = _cart.asStateFlow()

    /** Cart changes run one at a time, so quick taps on "+" can't overwrite each other. */
    private val mutex = Mutex()

    override suspend fun refresh(): DataResult<Cart> = change { lines -> DataResult.Success(lines) }

    override suspend fun add(productId: String, quantity: Int): DataResult<Cart> = change { lines ->
        // Only products that exist can go in the cart.
        when (val product = catalogue.product(productId)) {
            is DataResult.Failure -> product
            is DataResult.Success -> DataResult.Success(
                lines.toMutableMap().apply { this[productId] = clamp((this[productId] ?: 0) + quantity) },
            )
        }
    }

    override suspend fun setQuantity(productId: String, quantity: Int): DataResult<Cart> =
        if (quantity <= 0) remove(productId) else change { lines -> DataResult.Success(lines + (productId to clamp(quantity))) }

    override suspend fun remove(productId: String): DataResult<Cart> = change { lines -> DataResult.Success(lines - productId) }

    override suspend fun submitEnquiry(request: CartEnquiryRequest): DataResult<EnquiryReceipt> = mutex.withLock {
        val user = session.user.value ?: return@withLock signedOut()
        val lines = when (val result = linesFor(user)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return@withLock result
        }
        val cart = when (val result = price(lines)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return@withLock result
        }
        if (cart.isEmpty) return@withLock DataResult.Failure(DataError.Server(message = "Your cart is empty."))

        val receipt = newReceipt(clock)
        val sent = sender.submit(CartEnquiryFields.of(user, cart, request, receipt))
        if (sent is DataResult.Failure) return@withLock sent
        // Confirmed sent: the cart is done.
        store.save(user.id, emptyMap())
        _cart.value = Cart(items = emptyList(), itemCount = 0, total = Money(0.0, cart.total.currencyCode))
        DataResult.Success(receipt)
    }

    /** Loads the lines, applies [edit], saves them if they changed, and returns the priced cart. */
    private suspend fun change(edit: suspend (Map<String, Int>) -> DataResult<Map<String, Int>>): DataResult<Cart> = mutex.withLock {
        val user = session.user.value ?: return@withLock signedOut()
        val current = when (val result = linesFor(user)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return@withLock result
        }
        val updated = when (val result = edit(current)) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return@withLock result
        }
        if (updated != current) store.save(user.id, updated)
        price(updated).also { if (it is DataResult.Success) _cart.value = it.data }
    }

    /** This phone's lines for the user; the first time, their newest Fake Store cart. */
    private suspend fun linesFor(user: User): DataResult<Map<String, Int>> {
        store.load(user.id)?.let { return DataResult.Success(it) }
        val carts = when (val result = safeApiCall { api.cartsOfUser(user.id) }) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return result
        }
        // ISO dates sort as text, so the newest cart wins.
        val newest = carts.maxByOrNull { it.date }
        val lines = LinkedHashMap<String, Int>()
        newest?.products?.filter { it.quantity > 0 }?.forEach { line ->
            val id = line.productId.toString()
            lines[id] = clamp((lines[id] ?: 0) + line.quantity)
        }
        store.save(user.id, lines)
        return DataResult.Success(lines)
    }

    /** Prices every line from the live catalogue. A product Fake Store no longer has is dropped quietly. */
    private suspend fun price(lines: Map<String, Int>): DataResult<Cart> {
        val items = mutableListOf<CartItem>()
        for ((id, quantity) in lines) {
            when (val result = catalogue.product(id)) {
                is DataResult.Success -> {
                    val price = result.data.price
                    items += CartItem(result.data, quantity, Money(cents(price.amount * quantity), price.currencyCode))
                }
                is DataResult.Failure -> if (result.error != DataError.NotFound) return result
            }
        }
        val currency = items.firstOrNull()?.lineTotal?.currencyCode ?: pricing.currencyCode
        return DataResult.Success(
            Cart(
                items = items,
                itemCount = items.sumOf { it.quantity },
                total = Money(cents(items.sumOf { it.lineTotal.amount }), currency),
            ),
        )
    }

    private fun signedOut(): DataResult.Failure {
        _cart.value = null
        return DataResult.Failure(DataError.Unauthorized)
    }

    private fun clamp(quantity: Int) = quantity.coerceIn(1, Cart.MAX_QUANTITY)

    /** Rounds to the cent (half up), so 3 × 19.99 shows 59.97, never 59.970000000000006. */
    private fun cents(amount: Double): Double = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
}

/** What Formspree receives (and emails to sales) for a cart enquiry: one field per fact, plus every line. */
internal object CartEnquiryFields {
    fun of(user: User, cart: Cart, request: CartEnquiryRequest, receipt: EnquiryReceipt): Map<String, String> {
        fun money(m: Money) = "${m.currencyCode} ${String.format(Locale.ROOT, "%.2f", m.amount)}"
        return buildMap {
            put("_subject", "New Cart Enquiry – ${cart.itemCount} items from ${user.fullName}")
            put("reference", receipt.reference)
            put("submittedAt", receipt.receivedAt)
            put("name", user.fullName)
            put("email", user.email)
            put("phone", request.phone ?: user.phone.orEmpty())
            put("items", cart.items.joinToString("\n") { "${it.quantity} × ${it.product.title} (ID ${it.product.id}): ${money(it.lineTotal)}" })
            put("itemCount", cart.itemCount.toString())
            put("total", money(cart.total))
            put("message", request.message.orEmpty())
        }
    }
}
