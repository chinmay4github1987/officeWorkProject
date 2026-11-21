package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Width breakpoints (Material window size classes), measured on the app's window, not the device:
 * a phone, a folded foldable or a narrow Chromebook window is Compact; an unfolded foldable or a
 * portrait tablet is Medium; a landscape tablet, a desktop window or a TV is Expanded.
 */
object WindowWidth {
    val Medium = 600.dp
    val Expanded = 840.dp

    /** The current window width in dp. Updates when a foldable unfolds or a Chromebook window is resized. */
    @Composable
    fun current(): Dp = LocalConfiguration.current.screenWidthDp.dp

    /** How many columns a grid of cards should use: 2 on phones, 3 on medium windows, 4 on wide ones. */
    @Composable
    fun gridColumns(): Int = current().let { if (it >= Expanded) 4 else if (it >= Medium) 3 else 2 }
}

/** Widest a form or block of text should get. Longer lines are hard to read and fields look stretched. */
val ReadableWidth = 640.dp

/**
 * Full width on phones; centred and capped at [max] on tablets, foldables, Chromebooks and TVs.
 * Use it on forms, lists and long text so they don't stretch edge to edge on big screens.
 */
fun Modifier.readableWidth(max: Dp = ReadableWidth): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()

/**
 * A gold outline while focused, for D-pad (Android TV) and keyboard (Chromebook) navigation, where there is
 * no finger to show what's selected. Pass the same [interactionSource] the clickable uses.
 */
fun Modifier.focusRing(interactionSource: InteractionSource, shape: Shape): Modifier = composed {
    val focused by interactionSource.collectIsFocusedAsState()
    if (focused) border(2.dp, MaterialTheme.colorScheme.primary, shape) else this
}
