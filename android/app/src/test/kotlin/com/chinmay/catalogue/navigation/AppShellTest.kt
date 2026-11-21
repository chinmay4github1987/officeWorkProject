package com.chinmay.catalogue.navigation

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import com.chinmay.catalogue.core.data.repository.ThemeMode
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.testing.FakeAuthRepository
import com.chinmay.catalogue.core.testing.FakeCartRepository
import com.chinmay.catalogue.core.testing.FakeCatalogueRepository
import com.chinmay.catalogue.core.testing.FakeThemeRepository
import com.chinmay.catalogue.core.testing.MainDispatcherRule
import com.chinmay.catalogue.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The side drawer: its ViewModel (account, cart count, categories, theme) and its sun/moon switch on screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h2400dp")
class AppShellTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val compose = createComposeRule()

    private val auth = FakeAuthRepository()
    private val carts = FakeCartRepository(auth)
    private val themes = FakeThemeRepository()

    @Test
    fun `the switch saves dark or light, and the drawer shows the account, cart and categories`() = runTest {
        val viewModel = AppShellViewModel(auth, carts, FakeCatalogueRepository(), themes)

        viewModel.setDarkTheme(true)
        assertEquals(ThemeMode.DARK, themes.mode.value)
        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)
        viewModel.setDarkTheme(false)
        assertEquals(ThemeMode.LIGHT, themes.mode.value)

        auth.signIn()
        carts.add("3", 2)
        val state = viewModel.state.value
        assertEquals(TestData.user, state.user)
        assertEquals(2, state.cartCount)
        assertEquals(TestData.home.categories, state.categories)

        viewModel.signOut()
        assertEquals(0, viewModel.state.value.cartCount)
    }

    @Test
    // Clicks go through the accessibility action (what TalkBack and switch access use): Robolectric does not deliver
    // simulated touches inside a drawer sheet. Real taps are checked on the emulator.
    fun `tapping the moon and the sun picks dark and light`() {
        var dark: Boolean? = null
        var opened: DrawerDestination? = null
        compose.setContent {
            CatalogueTheme {
                AppDrawer(
                    state = DrawerUiState(categories = TestData.home.categories),
                    onNavigate = { opened = it },
                    onDarkThemeChange = { dark = it },
                    onSignOut = {},
                )
            }
        }

        compose.onNodeWithText("Light").assertIsSelected()
        compose.onNodeWithText("Dark").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(true, dark)
        compose.onNodeWithText("Light").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(false, dark)

        compose.onNodeWithText("Men's clothing").assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(DrawerDestination.CategoryPage(TestData.mens), opened)
    }
}
