package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chinmay.catalogue.core.designsystem.R

/*
 * Brand typography: two typefaces, bundled (no network, works on TV and Chromebooks without Play services).
 *  - Playfair Display, a high-contrast serif, for display and headline text: page titles, product names on
 *    the detail page, thank-you messages. It carries the "luxe" feel, so it is only used large.
 *  - Manrope, a calm geometric sans, for everything people read quickly or tap: body, prices, labels, buttons.
 * Both are variable fonts (one file, any weight). Licence: core/designsystem/FONT_LICENSE_OFL.txt.
 */
@OptIn(ExperimentalTextApi::class) // Stable in later Compose; needed here to pick a weight from a variable font.
private fun variable(resId: Int, weight: FontWeight) =
    Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

private val DisplayFontFamily = FontFamily(
    variable(R.font.playfair_display, FontWeight.Normal),
    variable(R.font.playfair_display, FontWeight.Medium),
    variable(R.font.playfair_display, FontWeight.SemiBold),
    variable(R.font.playfair_display, FontWeight.Bold),
)

private val BodyFontFamily = FontFamily(
    variable(R.font.manrope, FontWeight.Normal),
    variable(R.font.manrope, FontWeight.Medium),
    variable(R.font.manrope, FontWeight.SemiBold),
    variable(R.font.manrope, FontWeight.Bold),
)

/*
 * Every Material role is defined, so no screen falls back to defaults. Sizes in sp follow the user's font-size
 * setting. Smallest text is 12sp (11sp only for badges); body copy is 15–16sp with 1.5× line height for
 * comfortable reading; serif headlines get slightly negative tracking, small sans labels get wide tracking.
 *
 *   display  48 / 40 / 32  serif    · headline 28 / 24 / 21 serif
 *   title    18 / 16 / 14  sans     · body     16 / 15 / 13 sans · label 14 / 12 / 11 sans
 */
internal val CatalogueTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.6).sp),
    displayMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 28.sp, letterSpacing = (-0.1).sp),
    titleLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp),
    bodySmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp),
    // Category names and "eyebrow" labels above titles: small, uppercase in use, widely tracked.
    labelMedium = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp),
    labelSmall = TextStyle(fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
)
