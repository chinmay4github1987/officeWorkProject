package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.chinmay.catalogue.core.designsystem.theme.Spacing

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        // Marked as a heading so screen-reader users can jump between sections.
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
