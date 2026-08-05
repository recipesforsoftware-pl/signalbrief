package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefColors
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefTypography

private val LightColorScheme =
    lightColorScheme(
        primary = SignalBriefColors.primaryLight,
        onPrimary = SignalBriefColors.onPrimaryLight,
        primaryContainer = SignalBriefColors.primaryContainerLight,
        onPrimaryContainer = SignalBriefColors.onPrimaryContainerLight,
        secondary = SignalBriefColors.secondaryLight,
        onSecondary = SignalBriefColors.onSecondaryLight,
        secondaryContainer = SignalBriefColors.secondaryContainerLight,
        onSecondaryContainer = SignalBriefColors.onSecondaryContainerLight,
        tertiary = SignalBriefColors.tertiaryLight,
        onTertiary = SignalBriefColors.onTertiaryLight,
        tertiaryContainer = SignalBriefColors.tertiaryContainerLight,
        onTertiaryContainer = SignalBriefColors.onTertiaryContainerLight,
        error = SignalBriefColors.errorLight,
        onError = SignalBriefColors.onErrorLight,
        errorContainer = SignalBriefColors.errorContainerLight,
        onErrorContainer = SignalBriefColors.onErrorContainerLight,
        background = SignalBriefColors.backgroundLight,
        onBackground = SignalBriefColors.onBackgroundLight,
        surface = SignalBriefColors.surfaceLight,
        onSurface = SignalBriefColors.onSurfaceLight,
        surfaceVariant = SignalBriefColors.surfaceVariantLight,
        onSurfaceVariant = SignalBriefColors.onSurfaceVariantLight,
        surfaceContainerLow = SignalBriefColors.surfaceContainerLowLight,
        surfaceContainer = SignalBriefColors.surfaceContainerLight,
        outline = SignalBriefColors.outlineLight,
        outlineVariant = SignalBriefColors.outlineVariantLight,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = SignalBriefColors.primaryDark,
        onPrimary = SignalBriefColors.onPrimaryDark,
        primaryContainer = SignalBriefColors.primaryContainerDark,
        onPrimaryContainer = SignalBriefColors.onPrimaryContainerDark,
        secondary = SignalBriefColors.secondaryDark,
        onSecondary = SignalBriefColors.onSecondaryDark,
        secondaryContainer = SignalBriefColors.secondaryContainerDark,
        onSecondaryContainer = SignalBriefColors.onSecondaryContainerDark,
        tertiary = SignalBriefColors.tertiaryDark,
        onTertiary = SignalBriefColors.onTertiaryDark,
        tertiaryContainer = SignalBriefColors.tertiaryContainerDark,
        onTertiaryContainer = SignalBriefColors.onTertiaryContainerDark,
        error = SignalBriefColors.errorDark,
        onError = SignalBriefColors.onErrorDark,
        errorContainer = SignalBriefColors.errorContainerDark,
        onErrorContainer = SignalBriefColors.onErrorContainerDark,
        background = SignalBriefColors.backgroundDark,
        onBackground = SignalBriefColors.onBackgroundDark,
        surface = SignalBriefColors.surfaceDark,
        onSurface = SignalBriefColors.onSurfaceDark,
        surfaceVariant = SignalBriefColors.surfaceVariantDark,
        onSurfaceVariant = SignalBriefColors.onSurfaceVariantDark,
        surfaceContainerLow = SignalBriefColors.surfaceContainerLowDark,
        surfaceContainer = SignalBriefColors.surfaceContainerDark,
        outline = SignalBriefColors.outlineDark,
        outlineVariant = SignalBriefColors.outlineVariantDark,
    )

/**
 * Shared Material 3 theme used by iOS and by previews/tests.
 *
 * It applies the SignalBrief editorial palette and typography, follows the
 * system dark-mode setting by default, and avoids dynamic color so the brand
 * presentation stays consistent across platforms. The Android host can choose
 * to keep dynamic color off by default (see [SignalBriefAndroidTheme]) so both platforms
 * visibly belong to the same product.
 */
@Composable
fun SignalBriefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = SignalBriefTypography.materialTypography,
        content = content,
    )
}
