package pl.recipesforsoftware.signalbrief.ui.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Editorial color palette for SignalBrief.
 *
 * The palette is intentionally restrained: a deep ink primary, warm neutral
 * backgrounds, and muted accents. It avoids bright, generic dashboard colors
 * so the feed feels calm and readable in both light and dark mode.
 */
object SignalBriefColors {
    // Primary — Deep Ink (used for key actions, source labels, and emphasis)
    val primaryLight: Color = Color(0xFF1A1A2E)
    val onPrimaryLight: Color = Color(0xFFFFFFFF)
    val primaryContainerLight: Color = Color(0xFFE3E2E8)
    val onPrimaryContainerLight: Color = Color(0xFF1A1A2E)

    val primaryDark: Color = Color(0xFFE8E6E1)
    val onPrimaryDark: Color = Color(0xFF1A1A2E)
    val primaryContainerDark: Color = Color(0xFF2E2E3E)
    val onPrimaryContainerDark: Color = Color(0xFFE8E6E1)

    // Secondary — Steel Blue (used for secondary actions and subtle highlights)
    val secondaryLight: Color = Color(0xFF4A6C8C)
    val onSecondaryLight: Color = Color(0xFFFFFFFF)
    val secondaryContainerLight: Color = Color(0xFFD2E3F3)
    val onSecondaryContainerLight: Color = Color(0xFF0F283D)

    val secondaryDark: Color = Color(0xFF8AB4C7)
    val onSecondaryDark: Color = Color(0xFF0F283D)
    val secondaryContainerDark: Color = Color(0xFF274A5E)
    val onSecondaryContainerDark: Color = Color(0xFFD2E3F3)

    // Tertiary — Warm Sand (used sparingly for tertiary accents)
    val tertiaryLight: Color = Color(0xFF8C6B4A)
    val onTertiaryLight: Color = Color(0xFFFFFFFF)
    val tertiaryContainerLight: Color = Color(0xFFEFE1D3)
    val onTertiaryContainerLight: Color = Color(0xFF2E1F11)

    val tertiaryDark: Color = Color(0xFFC7B299)
    val onTertiaryDark: Color = Color(0xFF2E1F11)
    val tertiaryContainerDark: Color = Color(0xFF5C4633)
    val onTertiaryContainerDark: Color = Color(0xFFEFE1D3)

    // Error
    val errorLight: Color = Color(0xFF8B1A1A)
    val onErrorLight: Color = Color(0xFFFFFFFF)
    val errorContainerLight: Color = Color(0xFFFFDAD6)
    val onErrorContainerLight: Color = Color(0xFF410002)

    val errorDark: Color = Color(0xFFFFB4AB)
    val onErrorDark: Color = Color(0xFF690005)
    val errorContainerDark: Color = Color(0xFF93000A)
    val onErrorContainerDark: Color = Color(0xFFFFDAD6)

    // Background & Surface — warm off-white in light, near-black in dark
    val backgroundLight: Color = Color(0xFFF8F7F4)
    val onBackgroundLight: Color = Color(0xFF1A1A1E)
    val surfaceLight: Color = Color(0xFFFFFFFF)
    val onSurfaceLight: Color = Color(0xFF1A1A1E)
    val surfaceVariantLight: Color = Color(0xFFEFEDEA)
    val onSurfaceVariantLight: Color = Color(0xFF5E5C58)
    val surfaceContainerLowLight: Color = Color(0xFFF6F5F2)
    val surfaceContainerLight: Color = Color(0xFFEFEDEA)

    val backgroundDark: Color = Color(0xFF121212)
    val onBackgroundDark: Color = Color(0xFFE3E2DE)
    val surfaceDark: Color = Color(0xFF1E1E1E)
    val onSurfaceDark: Color = Color(0xFFE3E2DE)
    val surfaceVariantDark: Color = Color(0xFF2D2D2D)
    val onSurfaceVariantDark: Color = Color(0xFFB0ACA6)
    val surfaceContainerLowDark: Color = Color(0xFF1A1A1A)
    val surfaceContainerDark: Color = Color(0xFF262626)

    // Outline
    val outlineLight: Color = Color(0xFF74746F)
    val outlineVariantLight: Color = Color(0xFFC8C6C2)
    val outlineDark: Color = Color(0xFF8E8A84)
    val outlineVariantDark: Color = Color(0xFF474643)
}
