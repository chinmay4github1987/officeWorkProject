package com.chinmay.catalogue.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CartRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.ThemeMode
import com.chinmay.catalogue.core.data.repository.ThemeRepository
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the side drawer shows. */
data class DrawerUiState(
    val user: User? = null,
    val cartCount: Int = 0,
    val categories: List<Category> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.LIGHT,
)

/** Backs the app-wide side drawer: account, cart count, categories to jump to, and the light/dark switch. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val auth: AuthRepository,
    carts: CartRepository,
    private val catalogue: CatalogueRepository,
    private val themes: ThemeRepository,
) : ViewModel() {

    private val categories = MutableStateFlow<List<Category>>(emptyList())

    val state: StateFlow<DrawerUiState> = combine(auth.currentUser, carts.cart, categories, themes.mode) { user, cart, categories, mode ->
        DrawerUiState(user, if (user == null) 0 else cart?.itemCount ?: 0, categories, mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DrawerUiState(user = auth.currentUser.value, themeMode = themes.mode.value))

    init {
        loadCategories()
    }

    /** Called when the drawer opens, so a failed first load (e.g. offline at start) gets another try. */
    fun onDrawerOpened() {
        if (categories.value.isEmpty()) loadCategories()
    }

    fun setDarkTheme(dark: Boolean) = themes.setMode(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)

    fun signOut() = auth.logout()

    private fun loadCategories() {
        viewModelScope.launch {
            // Served from the catalogue's cache when Home has already loaded it.
            (catalogue.home() as? DataResult.Success)?.data?.categories?.let { categories.value = it }
        }
    }
}
