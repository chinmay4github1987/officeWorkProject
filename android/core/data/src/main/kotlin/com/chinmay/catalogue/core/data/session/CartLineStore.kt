package com.chinmay.catalogue.core.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Each customer's cart lines (product ID -> quantity, in the order they were added), kept on this phone.
 * Fake Store accepts cart changes but never saves them, so the phone is where a cart lives.
 * Prices are never stored: they're looked up fresh every time the cart is shown.
 */
internal interface CartLineStore {
    /** Null = never set up for this user on this phone (start from their Fake Store cart). */
    fun load(userId: String): Map<String, Int>?
    fun save(userId: String, lines: Map<String, Int>)
}

@Singleton
internal class SharedPreferencesCartLineStore @Inject constructor(
    @ApplicationContext context: Context,
) : CartLineStore {

    private val prefs = context.getSharedPreferences("carts", Context.MODE_PRIVATE)

    override fun load(userId: String): Map<String, Int>? =
        prefs.getString(key(userId), null)?.let(::decode)

    override fun save(userId: String, lines: Map<String, Int>) {
        prefs.edit().putString(key(userId), encode(lines)).apply()
    }

    private fun key(userId: String) = "lines_$userId"

    internal companion object {
        /** "1:4,2:1": product IDs are numbers and quantities are 1..99, so no escaping is needed. */
        fun encode(lines: Map<String, Int>): String = lines.entries.joinToString(",") { "${it.key}:${it.value}" }

        fun decode(raw: String): Map<String, Int> = raw.split(',')
            .mapNotNull { entry ->
                val id = entry.substringBefore(':', "").trim()
                val quantity = entry.substringAfter(':', "").trim().toIntOrNull()
                if (id.isEmpty() || quantity == null || quantity <= 0) null else id to quantity
            }
            .toMap(LinkedHashMap())
    }
}
