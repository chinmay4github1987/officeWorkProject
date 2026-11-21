package com.chinmay.catalogue.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The look the customer picked in the side drawer. */
enum class ThemeMode { LIGHT, DARK }

/** The light/dark choice, kept on this phone. Light (the warm daytime look) until the customer changes it. */
interface ThemeRepository {
    val mode: StateFlow<ThemeMode>
    fun setMode(mode: ThemeMode)
}

/** The customer's last searches on this phone, newest first, for one-tap repeat searches. */
interface RecentSearchesRepository {
    val recent: StateFlow<List<String>>
    fun add(term: String)
    fun clear()
}

@Singleton
internal class SharedPreferencesThemeRepository @Inject constructor(
    @ApplicationContext context: Context,
) : ThemeRepository {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(
        prefs.getString(KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.LIGHT,
    )
    override val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    override fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
    }

    private companion object {
        const val KEY = "theme_mode"
    }
}

@Singleton
internal class SharedPreferencesRecentSearchesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : RecentSearchesRepository {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    private val _recent = MutableStateFlow(
        prefs.getString(KEY, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty(),
    )
    override val recent: StateFlow<List<String>> = _recent.asStateFlow()

    override fun add(term: String) = save(withSearch(_recent.value, term))

    override fun clear() = save(emptyList())

    private fun save(terms: List<String>) {
        prefs.edit().putString(KEY, terms.joinToString("\n")).apply()
        _recent.value = terms
    }

    internal companion object {
        const val MAX = 8
        private const val KEY = "recent_searches"

        /** [term] first; an earlier copy (any case) removed; at most [MAX]. Blank terms aren't kept. */
        fun withSearch(current: List<String>, term: String): List<String> {
            val cleaned = term.trim().replace(Regex("\\s+"), " ")
            if (cleaned.isEmpty()) return current
            val key = cleaned.lowercase(Locale.ROOT)
            return (listOf(cleaned) + current.filter { it.lowercase(Locale.ROOT) != key }).take(MAX)
        }
    }
}
