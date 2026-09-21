package pl.recipesforsoftware.signalbrief.ui.designsystem.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Editorial typography scale for SignalBrief.
 *
 * Roles are mapped to the local prototype (`ui/shared/app.css`) while keeping the
 * existing Material 3 [Typography] API. The current implementation uses the
 * system default font family because the custom brand fonts (Source Serif 4 for
 * display/article roles, DM Sans for UI roles) are not packaged in this PR.
 *
 * Prototype roles and their current Material stand-ins:
 * - display 28-34px -> displayLarge / displayMedium / displaySmall / headlineLarge
 * - title 22px -> headlineMedium / titleLarge
 * - article 20px -> headlineSmall
 * - h3 16px -> titleMedium
 * - body 15px -> bodyLarge / bodyMedium
 * - meta 12px -> bodySmall
 * - label 12px -> labelMedium
 * - caption 11px -> labelSmall
 * - btn 15px -> labelLarge
 */
object SignalBriefTypography {
    private val defaultFontFamily: FontFamily = FontFamily.Default

    private val displayLarge =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.25).sp,
        )

    private val displayMedium =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.25).sp,
        )

    private val displaySmall =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).sp,
        )

    private val headlineLarge =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).sp,
        )

    private val headlineMedium =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).sp,
        )

    private val headlineSmall =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.02).sp,
        )

    private val titleLarge =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.02).sp,
        )

    private val titleMedium =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.01).sp,
        )

    private val titleSmall =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp,
        )

    private val bodyLarge =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        )

    private val bodyMedium =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        )

    private val bodySmall =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.01.sp,
        )

    private val labelLarge =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.02.sp,
        )

    private val labelMedium =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.06.sp,
        )

    private val labelSmall =
        TextStyle(
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.015.sp,
        )

    /** Material 3 [Typography] instance backed by the SignalBrief scale. */
    val materialTypography: Typography =
        Typography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            headlineLarge = headlineLarge,
            headlineMedium = headlineMedium,
            headlineSmall = headlineSmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
        )
}
