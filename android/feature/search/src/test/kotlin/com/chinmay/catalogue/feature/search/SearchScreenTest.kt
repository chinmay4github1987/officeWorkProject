package com.chinmay.catalogue.feature.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Search screen rendered on the JVM (Robolectric): what a shopper sees for each state. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3200dp")
class SearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var searched = 0
    private var picked: String? = null

    private fun show(state: SearchUiState) = compose.setContent {
        CatalogueTheme {
            SearchScreen(
                state = state,
                onQueryChange = {},
                onSearch = { searched++ },
                onSearchFor = { picked = it },
                onClear = {},
                onClearRecent = {},
                onCategorySelected = {},
                onLoadMore = {},
                onBack = {},
                onProductClick = {},
            )
        }
    }

    @Test
    fun `results show the count and the matching products in a grid`() {
        val ssds = TestData.products.filter { "SSD" in it.title }
        show(SearchUiState(query = "ssd", results = SearchResults.Found("ssd", ssds)))

        compose.onNodeWithText("Results for “ssd”").assertIsDisplayed()
        compose.onNodeWithText("2 items").assertIsDisplayed()
        compose.onNodeWithText("SanDisk SSD").assertIsDisplayed()
        compose.onNodeWithText("Silicon Power SSD").assertIsDisplayed()
    }

    @Test
    fun `no matches says so and offers popular searches`() {
        show(SearchUiState(query = "unicorn", results = SearchResults.Found("unicorn", emptyList())))

        compose.onNodeWithText("No matches for “unicorn”").assertIsDisplayed()
        compose.onNodeWithText("Popular searches").assertIsDisplayed()
    }

    @Test
    fun `the search button runs the search and a recent search can be repeated`() {
        show(SearchUiState(query = "ssd", recent = listOf("desk lamp")))

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("desk lamp").performClick()

        assertEquals(1, searched)
        assertEquals("desk lamp", picked)
    }
}
