package com.chinmay.catalogue.feature.category

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.onFirst
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Category screen rendered on the JVM (Robolectric): top rated first, then more, with "Load more". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h2400dp")
class CategoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows the banner, top rated, then more products with load more`() {
        var loadMore = 0
        val more = (20..27).map { TestData.product(it.toString(), "Gadget $it") }
        compose.setContent {
            CatalogueTheme {
                CategoryScreen(
                    category = TestData.electronics,
                    categories = listOf(TestData.electronics, TestData.mens),
                    state = CategoryUiState.Success(CategoryPage(TestData.electronics, TestData.electronicsPage.topRated, more)),
                    onBack = {},
                    onProductClick = {},
                    onCategorySelected = {},
                    onLoadMore = { loadMore++ },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Collection").assertIsDisplayed()
        compose.onNodeWithText("Top rated in Electronics").assertIsDisplayed()
        compose.onNodeWithText("Portable Hard Drive").assertIsDisplayed()
        // The outer grid (the first scrollable); the category chip row inside it scrolls too.
        val grid = compose.onAllNodes(hasScrollToNodeAction()).onFirst()
        grid.performScrollToNode(hasText("More in Electronics"))
        compose.onNodeWithText("More in Electronics").assertIsDisplayed()
        grid.performScrollToNode(hasText("Load more (2 left)"))
        compose.onNodeWithText("Load more (2 left)").performClick()
        assertEquals(1, loadMore)
    }
}
