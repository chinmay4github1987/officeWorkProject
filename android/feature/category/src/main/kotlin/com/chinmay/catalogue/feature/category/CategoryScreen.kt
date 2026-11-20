package com.chinmay.catalogue.feature.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.SectionHeader
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.FeaturedProductCard
import com.chinmay.catalogue.core.ui.ProductTile
import com.chinmay.catalogue.core.ui.ProductTileSkeleton

@Composable
internal fun CategoryRoute(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryScreen(viewModel.title, state, onBack, onProductClick, viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoryScreen(
    title: String,
    state: CategoryUiState,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    // The large title collapses as you scroll, so the products get the space.
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val name = (state as? CategoryUiState.Success)?.page?.category?.name ?: title
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_category_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (state) {
                CategoryUiState.Loading -> items(6) { ProductTileSkeleton() }
                is CategoryUiState.Error -> fullWidth { DataErrorState(state.error, onRetry) }
                is CategoryUiState.Success -> {
                    val page = state.page
                    fullWidth { SectionHeader(stringResource(R.string.feature_category_top_rated, page.category.name), Modifier.bleed(Spacing.lg)) }
                    fullWidth {
                        // Full-bleed carousel: undo the grid's side padding so cards scroll edge to edge.
                        LazyRow(
                            modifier = Modifier.bleed(Spacing.lg),
                            contentPadding = PaddingValues(horizontal = Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            itemsIndexed(page.topRated, key = { _, p -> p.id }) { index, product ->
                                FeaturedProductCard(product, rank = index + 1, onClick = { onProductClick(product.id) })
                            }
                        }
                    }
                    if (page.more.isNotEmpty()) {
                        fullWidth {
                            SectionHeader(
                                stringResource(R.string.feature_category_more, page.category.name),
                                Modifier.bleed(Spacing.lg),
                                subtitle = stringResource(R.string.feature_category_count, page.more.size),
                            )
                        }
                        items(page.more, key = { it.id }) { product ->
                            ProductTile(product, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/**
 * Lets a full-width grid item extend past the grid's side padding (padding can't be negative,
 * so this re-measures the child wider and shifts it left).
 */
private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val width = constraints.maxWidth + extra * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}
