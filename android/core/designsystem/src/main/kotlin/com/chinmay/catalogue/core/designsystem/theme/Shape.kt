package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

internal val CatalogueShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp), // product tiles and cards, as in the reference
    extraLarge = RoundedCornerShape(32.dp),
)

/** Fully rounded ends: search bar, chips, buttons, quantity stepper and the size picker, as in the reference. */
val PillShape = RoundedCornerShape(percent = 50)

/** 14dp card shape — slightly softer than shapes.large (20dp) for product grid cards. */
val CardShape = RoundedCornerShape(14.dp)

/** One spacing scale for every screen, so layouts stay consistent. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
