package com.chinmay.catalogue.feature.cart

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.designsystem.component.PrimaryButton
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Cart
import com.chinmay.catalogue.core.model.CartItem
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.errorText
import com.chinmay.catalogue.core.ui.formatted

@Composable
internal fun CartRoute(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onProductClick: (String) -> Unit,
    onContinueBrowsing: () -> Unit,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CartScreen(
        state = state,
        onBack = onBack,
        onSignIn = onSignIn,
        onProductClick = onProductClick,
        onContinueBrowsing = onContinueBrowsing,
        onQuantityChange = viewModel::onQuantityChange,
        onRemove = viewModel::onRemove,
        onMessageChange = viewModel::onMessageChange,
        onSendEnquiry = viewModel::onSendEnquiry,
        onDismissError = viewModel::dismissError,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartScreen(
    state: CartUiState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onProductClick: (String) -> Unit,
    onContinueBrowsing: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSendEnquiry: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (state !is CartUiState.Sent) Text(stringResource(R.string.feature_cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_cart_back))
                    }
                },
            )
        },
        // The total and the call to action stay visible however long the cart is.
        bottomBar = {
            if (state is CartUiState.Ready && !state.cart.isEmpty) SendBar(state, onSendEnquiry)
        },
    ) { padding ->
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "cartContent",
            modifier = Modifier.padding(padding),
        ) { current ->
            when (current) {
                CartUiState.Loading -> LoadingContent()
                CartUiState.SignedOut -> CallToAction(
                    title = stringResource(R.string.feature_cart_signed_out_title),
                    message = stringResource(R.string.feature_cart_signed_out_message),
                    action = stringResource(R.string.feature_cart_sign_in),
                    onAction = onSignIn,
                    icon = Icons.Filled.Lock,
                )
                is CartUiState.Error -> DataErrorState(current.error, onRetry)
                is CartUiState.Ready ->
                    if (current.cart.isEmpty) {
                        CallToAction(
                            title = stringResource(R.string.feature_cart_empty_title),
                            message = stringResource(R.string.feature_cart_empty_message),
                            action = stringResource(R.string.feature_cart_browse),
                            onAction = onContinueBrowsing,
                            icon = Icons.Filled.ShoppingCart,
                        )
                    } else {
                        CartContent(current, onProductClick, onQuantityChange, onRemove, onMessageChange, onDismissError)
                    }
                is CartUiState.Sent -> Sent(current, onContinueBrowsing)
            }
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Ready,
    onProductClick: (String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Text(
                pluralStringResource(R.plurals.feature_cart_item_count, state.cart.itemCount, state.cart.itemCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        items(state.cart.items, key = { it.product.id }) { item ->
            CartItemRow(
                item = item,
                isUpdating = item.product.id in state.updating,
                onClick = { onProductClick(item.product.id) },
                onQuantityChange = { onQuantityChange(item.product.id, it) },
                onRemove = { onRemove(item.product.id) },
                modifier = Modifier.animateItem(),
            )
        }
        state.actionError?.let { error -> item { ErrorBanner(error, onDismissError) } }
        item {
            OutlinedTextField(
                value = state.message,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.feature_cart_message)) },
                placeholder = { Text(stringResource(R.string.feature_cart_message_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                minLines = 3,
                enabled = !state.isSending,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    isUpdating: Boolean,
    onClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProductImage(
                url = item.product.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clickable(onClick = onClick),
                padding = PaddingValues(8.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        item.product.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable(onClick = onClick),
                    )
                    IconButton(onClick = onRemove, enabled = !isUpdating, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.feature_cart_remove, item.product.title),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.feature_cart_each, item.product.price.formatted()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    QuantityStepper(item.quantity, item.product.title, isUpdating, onQuantityChange)
                    Text(
                        item.lineTotal.formatted(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** − n +. At 1, minus becomes a bin, so it's obvious the next tap removes the product. */
@Composable
private fun QuantityStepper(quantity: Int, title: String, isUpdating: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        FilledTonalIconButton(onClick = { onChange(quantity - 1) }, enabled = !isUpdating, modifier = Modifier.size(36.dp)) {
            if (quantity <= 1) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.feature_cart_remove, title), modifier = Modifier.size(18.dp))
            } else {
                val label = stringResource(R.string.feature_cart_decrease, title)
                Text("−", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { contentDescription = label })
            }
        }
        Text(
            quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        FilledTonalIconButton(
            onClick = { onChange(quantity + 1) },
            enabled = !isUpdating && quantity < Cart.MAX_QUANTITY,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.feature_cart_increase, title), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SendBar(state: CartUiState.Ready, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feature_cart_total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(state.cart.total.formatted(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            PrimaryButton(
                text = stringResource(R.string.feature_cart_send),
                onClick = onSend,
                icon = Icons.Filled.CheckCircle,
                enabled = state.updating.isEmpty(),
                loading = state.isSending,
                loadingText = stringResource(R.string.feature_cart_sending),
            )
            Text(
                stringResource(R.string.feature_cart_send_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ErrorBanner(error: com.chinmay.catalogue.core.model.DataError, onDismiss: () -> Unit) {
    val (title, message) = errorText(error)
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_cart_dismiss), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun CallToAction(title: String, message: String, action: String, onAction: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)) {
        MessageState(icon = icon, title = title, message = message)
        PrimaryButton(action, onClick = onAction)
    }
}

@Composable
private fun Sent(state: CartUiState.Sent, onContinueBrowsing: () -> Unit) {
    // After sending, Back leaves the cart instead of showing the thank-you again.
    BackHandler(onBack = onContinueBrowsing)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        Text(
            stringResource(R.string.feature_cart_sent_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        Text(
            stringResource(R.string.feature_cart_sent_body, state.email),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
            Text(
                stringResource(R.string.feature_cart_reference, state.receipt.reference),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        PrimaryButton(stringResource(R.string.feature_cart_browse), onClick = onContinueBrowsing, modifier = Modifier.padding(top = Spacing.sm))
    }
}

@Composable
private fun LoadingContent() {
    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SkeletonBox(Modifier.fillMaxWidth(0.3f).height(20.dp))
        repeat(3) { SkeletonBox(Modifier.fillMaxWidth().height(112.dp), MaterialTheme.shapes.large) }
    }
}

@Preview
@Composable
private fun CartScreenPreview() {
    val product = Product("3", "Cotton Jacket", "", Money(55.99, "USD"), Category("mens-clothing", "Men's clothing"), "", Rating(4.7, 500))
    val cart = Cart(listOf(CartItem(product, 2, Money(111.98, "USD"))), itemCount = 2, total = Money(111.98, "USD"))
    CatalogueTheme {
        CartScreen(CartUiState.Ready(cart), {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}
