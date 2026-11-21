package com.chinmay.catalogue.feature.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.CategoryChip
import com.chinmay.catalogue.core.designsystem.component.CircleIconButton
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CategoryPage
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductCard
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.ProductTileSkeleton

@Composable
internal fun CategoryRoute(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    CategoryScreen(
        category = category,
        categories = categories,
        state = state,
        onBack = onBack,
        onProductClick = onProductClick,
        onCategorySelected = viewModel::selectCategory,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
    )
}

/**
 * A category: header, a pastel banner with its description and best product, chips to switch category,
 * "Top rated" first and then "More in …" with "Load more". Products never repeat between the two sections.
 */
@Composable
internal fun CategoryScreen(
    category: Category,
    categories: List<Category>,
    state: CategoryUiState,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                CircleIconButton(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(R.string.feature_category_back),
                    onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    category.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp).semantics { heading() },
                )
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
            item(span = full, key = "banner") {
                Banner(category, (state as? CategoryUiState.Success)?.page, (state as? CategoryUiState.Success)?.productCount)
            }
            if (categories.size > 1) {
                item(span = full, key = "chips") { Chips(categories, category, onCategorySelected) }
            }
            when (state) {
                CategoryUiState.Loading -> items(4) { ProductTileSkeleton() }
                is CategoryUiState.Error -> item(span = full) { DataErrorState(state.error, onRetry) }
                is CategoryUiState.Success -> when {
                    state.isEmpty -> item(span = full) {
                        MessageState(
                            Icons.Filled.Search,
                            stringResource(R.string.feature_category_empty_title),
                            stringResource(R.string.feature_category_empty_message),
                        )
                    }
                    else -> {
                        if (state.page.topRated.isNotEmpty()) {
                            item(span = full, key = "top-title") { SectionHeading(stringResource(R.string.feature_category_top_rated, category.name)) }
                            items(state.page.topRated, key = { "top-${it.id}" }) { product ->
                                ProductCard(product, onClick = { onProductClick(product.id) }, detailed = true)
                            }
                        }
                        if (state.page.more.isNotEmpty()) {
                            item(span = full, key = "more-title") { SectionHeading(stringResource(R.string.feature_category_more, category.name)) }
                            items(state.visibleMore, key = { "more-${it.id}" }) { product ->
                                ProductCard(product, onClick = { onProductClick(product.id) }, detailed = true)
                            }
                            if (state.remaining > 0) {
                                item(span = full, key = "load-more") {
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        shape = PillShape,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = Spacing.sm),
                                    ) {
                                        Text(pluralStringResource(R.plurals.feature_category_load_more, state.remaining, state.remaining))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The category banner: its pastel colour, a short description, how many products, and its best product's photo. */
@Composable
private fun Banner(category: Category, page: CategoryPage?, productCount: Int?) {
    val accents = CatalogueTheme.extendedColors.categoryAccents
    val (container, content) = accents[(category.slug.hashCode() and Int.MAX_VALUE) % accents.size]
    Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(22.dp)).background(container)) {
        Row(Modifier.fillMaxSize().padding(start = Spacing.lg, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.feature_category_eyebrow), style = MaterialTheme.typography.labelLarge, color = content)
                Text(category.name, style = MaterialTheme.typography.headlineSmall, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    categoryDescription(category.slug),
                    style = MaterialTheme.typography.bodySmall,
                    color = content,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (productCount != null) {
                    Text(
                        pluralStringResource(R.plurals.feature_category_count, productCount, productCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = content,
                    )
                }
            }
            val hero = page?.topRated?.firstOrNull()
            if (hero != null) {
                ProductImage(
                    url = hero.imageUrl,
                    contentDescription = null,
                    shape = MaterialTheme.shapes.large,
                    padding = PaddingValues(Spacing.sm + 2.dp),
                    modifier = Modifier.padding(start = Spacing.sm).size(112.dp),
                )
            } else {
                SkeletonBox(Modifier.padding(start = Spacing.sm).size(112.dp), MaterialTheme.shapes.large)
            }
        }
    }
}

/** Short shop copy for Fake Store's categories; anything new gets a friendly generic line. */
@Composable
private fun categoryDescription(slug: String): String = stringResource(
    when (slug) {
        "electronics" -> R.string.feature_category_about_electronics
        "jewelery" -> R.string.feature_category_about_jewelery
        "mens-clothing" -> R.string.feature_category_about_mens_clothing
        "womens-clothing" -> R.string.feature_category_about_womens_clothing
        else -> R.string.feature_category_about_generic
    },
)

@Composable
private fun Chips(categories: List<Category>, current: Category, onSelect: (Category) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), contentPadding = PaddingValues(vertical = Spacing.xs)) {
        items(categories, key = { it.slug }) { category ->
            CategoryChip(category.name, selected = category.slug == current.slug, onClick = { onSelect(category) }, icon = Icons.Filled.Check)
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = Spacing.md).semantics { heading() },
    )
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun CategoryScreenPreview() {
    val category = Category("mens-clothing", "Men's clothing")
    fun product(id: String) = Product(id, "Product $id", "", Money(19.99, "USD"), category, "", Rating(4.1, 120))
    CatalogueTheme {
        CategoryScreen(
            category = category,
            categories = listOf(category, Category("jewelery", "Jewelery")),
            state = CategoryUiState.Success(CategoryPage(category, listOf(product("1"), product("2")), listOf(product("3")))),
            onBack = {}, onProductClick = {}, onCategorySelected = {}, onLoadMore = {}, onRetry = {},
        )
    }
}
