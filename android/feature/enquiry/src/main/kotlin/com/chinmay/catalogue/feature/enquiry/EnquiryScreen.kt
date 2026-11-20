package com.chinmay.catalogue.feature.enquiry

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.errorText
import com.chinmay.catalogue.core.ui.formatted

@Composable
internal fun EnquiryRoute(
    onClose: () -> Unit,
    onContinueBrowsing: () -> Unit,
    viewModel: EnquiryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EnquiryScreen(
        state = state,
        onFieldChange = viewModel::onFieldChange,
        onSubmit = viewModel::onSubmit,
        onClose = onClose,
        onContinueBrowsing = onContinueBrowsing,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EnquiryScreen(
    state: EnquiryUiState,
    onFieldChange: (EnquiryField, String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onContinueBrowsing: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (state.receipt == null) Text(stringResource(R.string.feature_enquiry_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.feature_enquiry_close))
                    }
                },
            )
        },
    ) { padding ->
        // Cross-fade from the form to the thank-you view once the backend confirms.
        AnimatedContent(
            targetState = state.receipt != null,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "enquiryContent",
            modifier = Modifier.padding(padding),
        ) { submitted ->
            if (submitted) {
                ThankYou(state, onBackToProduct = onClose, onContinueBrowsing = onContinueBrowsing)
            } else {
                Form(state, onFieldChange, onSubmit)
            }
        }
    }
}

@Composable
private fun Form(state: EnquiryUiState, onFieldChange: (EnquiryField, String) -> Unit, onSubmit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        state.product?.let { ProductSummary(it) }
        Text(
            stringResource(R.string.feature_enquiry_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FormField(state, EnquiryField.Name, R.string.feature_enquiry_name, onFieldChange,
            KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next))
        FormField(state, EnquiryField.Email, R.string.feature_enquiry_email, onFieldChange,
            KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
        FormField(state, EnquiryField.Phone, R.string.feature_enquiry_phone, onFieldChange,
            KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next))
        FormField(state, EnquiryField.Message, R.string.feature_enquiry_message, onFieldChange,
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), singleLine = false,
            placeholder = stringResource(R.string.feature_enquiry_message_placeholder))

        state.submitError?.let { error ->
            val (title, message) = errorText(error)
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Column {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        Text(
            stringResource(R.string.feature_enquiry_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Text(stringResource(R.string.feature_enquiry_sending), modifier = Modifier.padding(start = Spacing.sm))
            } else {
                Text(stringResource(R.string.feature_enquiry_submit))
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(Spacing.lg))
    }
}

@Composable
private fun FormField(
    state: EnquiryUiState,
    field: EnquiryField,
    @StringRes label: Int,
    onFieldChange: (EnquiryField, String) -> Unit,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    val error = state.errors[field]
    OutlinedTextField(
        value = state.form.valueOf(field),
        onValueChange = { onFieldChange(field, it) },
        label = { Text(stringResource(label)) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        isError = error != null,
        supportingText = if (error != null) { { Text(errorMessage(error, field)) } } else null,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        enabled = !state.isSubmitting,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun errorMessage(error: FieldError, field: EnquiryField): String = when (error) {
    FieldError.Required -> stringResource(
        if (field == EnquiryField.Email) R.string.feature_enquiry_error_required_email else R.string.feature_enquiry_error_required_name,
    )
    FieldError.InvalidEmail -> stringResource(R.string.feature_enquiry_error_email)
    FieldError.InvalidPhone -> stringResource(R.string.feature_enquiry_error_phone)
    is FieldError.TooLong -> stringResource(R.string.feature_enquiry_error_too_long, error.max)
    is FieldError.Server -> error.message
}

@Composable
private fun ProductSummary(product: Product) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ProductImage(product.imageUrl, contentDescription = null, modifier = Modifier.size(64.dp), padding = PaddingValues(6.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(product.price.formatted(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ThankYou(state: EnquiryUiState, onBackToProduct: () -> Unit, onContinueBrowsing: () -> Unit) {
    val receipt = state.receipt ?: return
    // After sending, Back leaves the flow instead of returning to a form that was already submitted.
    BackHandler(onBack = onBackToProduct)

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "checkScale",
    )

    val firstName = state.form.name.trim().substringBefore(' ')
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(112.dp).scale(scale).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        Text(
            stringResource(R.string.feature_enquiry_thanks_title, firstName),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        Text(
            stringResource(R.string.feature_enquiry_thanks_body, state.product?.title ?: "this product", state.form.email.trim()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
            Text(
                stringResource(R.string.feature_enquiry_reference, receipt.reference),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Button(onClick = onBackToProduct, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(stringResource(R.string.feature_enquiry_back_to_product))
        }
        TextButton(onClick = onContinueBrowsing) { Text(stringResource(R.string.feature_enquiry_continue)) }
    }
}
