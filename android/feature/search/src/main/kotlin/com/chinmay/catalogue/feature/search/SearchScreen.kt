package com.chinmay.catalogue.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductTile
import com.chinmay.catalogue.core.ui.ProductTileSkeleton

@Composable
internal fun SearchRoute(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onClear = viewModel::onClear,
        onBack = onBack,
        onProductClick = onProductClick,
    )
}

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val submit = {
        keyboard?.hide()
        onSearch()
    }
    // Opening search means you want to type: focus the field and show the keyboard straight away.
    LaunchedEffect(Unit) { if (state.results == SearchResults.Idle) focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = Spacing.md).padding(horizontal = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_search_back))
                }
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.feature_search_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    isError = state.showRequiredError,
                    supportingText = if (state.showRequiredError) {
                        { Text(stringResource(R.string.feature_search_required)) }
                    } else null,
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.feature_search_clear))
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                )
                Button(
                    onClick = submit,
                    modifier = Modifier.padding(start = Spacing.sm, end = Spacing.sm).height(52.dp),
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(stringResource(R.string.feature_search_button))
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
            when (val results = state.results) {
                SearchResults.Idle -> item(span = full) {
                    MessageState(Icons.Filled.Search, stringResource(R.string.feature_search_idle_title), stringResource(R.string.feature_search_idle_message))
                }
                SearchResults.Loading -> items(6) { ProductTileSkeleton() }
                is SearchResults.Error -> item(span = full) { DataErrorState(results.error, onRetry = onSearch) }
                is SearchResults.Found -> {
                    if (results.products.isEmpty()) {
                        item(span = full) {
                            MessageState(
                                Icons.Filled.Search,
                                stringResource(R.string.feature_search_empty_title, results.term),
                                stringResource(R.string.feature_search_empty_message),
                            )
                        }
                    } else {
                        item(span = full) {
                            // Announced by screen readers when results arrive.
                            Text(
                                pluralStringResource(R.plurals.feature_search_results, results.products.size, results.products.size, results.term),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        items(results.products, key = { it.id }) { product ->
                            ProductTile(product, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }
    }
}
