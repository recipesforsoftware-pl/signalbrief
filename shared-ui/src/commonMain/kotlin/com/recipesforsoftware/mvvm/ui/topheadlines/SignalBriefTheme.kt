package com.recipesforsoftware.mvvm.ui.topheadlines

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF3F51B5),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBE1FF),
        onPrimaryContainer = Color(0xFF001849),
        secondary = Color(0xFF006B5E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF7FF8E3),
        onSecondaryContainer = Color(0xFF00201B),
        tertiary = Color(0xFF9C4065),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD9E3),
        onTertiaryContainer = Color(0xFF3F0020),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFDFBFF),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFDFBFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFE1E2EC),
        onSurfaceVariant = Color(0xFF44474E),
        surfaceContainerLow = Color(0xFFF6F6FA),
        surfaceContainer = Color(0xFFF0F0F7),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFB6C4FF),
        onPrimary = Color(0xFF002B78),
        primaryContainer = Color(0xFF1A3B8C),
        onPrimaryContainer = Color(0xFFDBE1FF),
        secondary = Color(0xFF5EDBC7),
        onSecondary = Color(0xFF003730),
        secondaryContainer = Color(0xFF005047),
        onSecondaryContainer = Color(0xFF7FF8E3),
        tertiary = Color(0xFFFFB0CC),
        onTertiary = Color(0xFF5F1136),
        tertiaryContainer = Color(0xFF7E294D),
        onTertiaryContainer = Color(0xFFFFD9E3),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF111318),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF111318),
        onSurface = Color(0xFFC5C6D0),
        surfaceVariant = Color(0xFF44474E),
        onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainerLow = Color(0xFF191B20),
        surfaceContainer = Color(0xFF1E2025),
    )

/**
 * Shared Material 3 theme used by non-Android hosts (iOS) and previews/tests.
 *
 * The Android host keeps its own [NewsAppTheme]-style wrapper (dynamic color
 * plus a persisted dark-mode toggle); this theme follows the system dark-mode
 * setting and mirrors the Android palette so both platforms look consistent.
 */
@Composable
fun SignalBriefTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
