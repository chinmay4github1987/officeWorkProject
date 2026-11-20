package com.chinmay.catalogue.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.model.DataError

/** One consistent way to show any [DataError], so every screen words failures the same way. */
@Composable
fun DataErrorState(error: DataError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val (title, message) = errorText(error)
    MessageState(
        icon = if (error is DataError.NotFound) Icons.Filled.Info else Icons.Filled.Warning,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = if (onRetry != null && error !is DataError.NotFound) stringResource(R.string.core_ui_retry) else null,
        onAction = onRetry,
    )
}

/** Short, user-facing wording for an error. Also used for inline banners. */
@Composable
fun errorText(error: DataError): Pair<String, String> = when (error) {
    DataError.Network -> stringResource(R.string.core_ui_error_network_title) to stringResource(R.string.core_ui_error_network_message)
    DataError.NotFound -> stringResource(R.string.core_ui_error_not_found_title) to stringResource(R.string.core_ui_error_not_found_message)
    DataError.RateLimited -> stringResource(R.string.core_ui_error_rate_limited_title) to stringResource(R.string.core_ui_error_rate_limited_message)
    is DataError.Server -> stringResource(R.string.core_ui_error_generic_title) to (error.message ?: stringResource(R.string.core_ui_error_generic_message))
    is DataError.Validation -> stringResource(R.string.core_ui_error_generic_title) to stringResource(R.string.core_ui_error_generic_message)
}
