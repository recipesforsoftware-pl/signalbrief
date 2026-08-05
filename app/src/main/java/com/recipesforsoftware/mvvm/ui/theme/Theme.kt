package com.recipesforsoftware.mvvm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefColors
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefTypography

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
    )

/**
 * Android host theme for SignalBrief.
 *
 * By default dynamic color is **disabled** so the editorial SignalBrief palette
 * is the consistent brand experience on both Android and iOS. Consumers can still
 * opt in to dynamic color by passing `dynamicColor = true`; on Android 12+ this
 * will override the shared palette with the system wallpaper-derived colors.
 *
 * Both static schemes consume the shared [SignalBriefColors] tokens directly, so
 * there is a single source of truth for the editorial palette across the app and
 * the shared UI.
 */
@Composable
fun NewsAppTheme(
    isDarkMode: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkMode ?: isSystemInDarkTheme()

    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColorScheme
            }

            else -> {
                LightColorScheme
            }
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SignalBriefTypography.materialTypography,
        content = content,
    )
}
