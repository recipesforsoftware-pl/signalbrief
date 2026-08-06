package pl.recipesforsoftware.signalbrief.ui.designsystem.tokens

import androidx.compose.ui.graphics.Color

/**
 * Editorial color palette for SignalBrief.
 *
 * Values are taken from the local prototype source of truth (`ui/shared/tokens.css`)
 * and converted from OKLch to sRGB hex. The palette is intentionally restrained:
 * a deep signal-blue primary, a bright blue secondary, warm orange used only for
 * tertiary accents, and calm light/dark surfaces with navy-ish text.
 *
 * Light OKLch sources (tokens.css):
 * - bg: oklch(98.5% 0.008 240) -> #F6FBFF
 * - surface: oklch(100% 0 0) -> #FFFFFF
 * - surface-2: oklch(97% 0.012 240) -> #EEF6FC
 * - fg: oklch(28% 0.05 255) -> #172A41
 * - muted: oklch(50% 0.03 250) -> #576574
 * - border: oklch(91% 0.015 240) -> #D9E3EA
 * - accent: oklch(50% 0.155 255) -> #1162B8
 * - accent-bright: oklch(66% 0.165 245) -> #0699EF
 * - soft: oklch(94% 0.03 240) -> #DAEEFE
 * - warm: oklch(66% 0.185 35) -> #ED5D3A
 * - danger: oklch(56% 0.19 25) -> #CC3336
 *
 * Dark OKLch sources (tokens.css):
 * - bg: oklch(17% 0.035 255) -> #05101E
 * - surface: oklch(21% 0.04 255) -> #0B192A
 * - surface-2: oklch(25% 0.04 255) -> #142234
 * - fg: oklch(94% 0.012 240) -> #E4ECF2
 * - muted: oklch(70% 0.025 250) -> #93A0AE
 * - border: oklch(32% 0.03 255) -> #293442
 * - soft: oklch(28% 0.05 250) -> #142A41
 * - accent: oklch(68% 0.14 245) -> #3F9FE8
 * - accent-bright: oklch(72% 0.15 240) -> #30AFF8
 * - warm: oklch(70% 0.17 40) -> #F37344
 */
object SignalBriefColors {
    // Primary — Signal Blue (CTAs, key actions, active states)
    val primaryLight: Color = Color(0xFF1162B8)
    val onPrimaryLight: Color = Color(0xFFFFFFFF)
    val primaryContainerLight: Color = Color(0xFFDAEEFE)
    val onPrimaryContainerLight: Color = Color(0xFF1162B8)

    val primaryDark: Color = Color(0xFF3F9FE8)
    val onPrimaryDark: Color = Color(0xFF05101E)
    val primaryContainerDark: Color = Color(0xFF142A41)
    val onPrimaryContainerDark: Color = Color(0xFF3F9FE8)

    // Secondary — Electric Blue (subtle highlights, info accents)
    val secondaryLight: Color = Color(0xFF0699EF)
    val onSecondaryLight: Color = Color(0xFFFFFFFF)
    val secondaryContainerLight: Color = Color(0xFFDAEEFE)
    val onSecondaryContainerLight: Color = Color(0xFF1162B8)

    val secondaryDark: Color = Color(0xFF30AFF8)
    val onSecondaryDark: Color = Color(0xFF05101E)
    val secondaryContainerDark: Color = Color(0xFF142A41)
    val onSecondaryContainerDark: Color = Color(0xFF30AFF8)

    // Tertiary — Warm Orange (used sparingly: badges, alerts, Pro markers)
    val tertiaryLight: Color = Color(0xFFED5D3A)
    val onTertiaryLight: Color = Color(0xFFFFFFFF)
    val tertiaryContainerLight: Color = Color(0xFFEEF6FC)
    val onTertiaryContainerLight: Color = Color(0xFFED5D3A)

    val tertiaryDark: Color = Color(0xFFF37344)
    val onTertiaryDark: Color = Color(0xFF05101E)
    val tertiaryContainerDark: Color = Color(0xFF142234)
    val onTertiaryContainerDark: Color = Color(0xFFF37344)

    // Error — mapped from the prototype danger token; dark error uses the
    // existing accessible Material dark pair because the prototype does not
    // define a dark-mode danger value.
    val errorLight: Color = Color(0xFFCC3336)
    val onErrorLight: Color = Color(0xFFFFFFFF)
    val errorContainerLight: Color = Color(0xFFFFDAD6)
    val onErrorContainerLight: Color = Color(0xFFCC3336)

    val errorDark: Color = Color(0xFFFFB4AB)
    val onErrorDark: Color = Color(0xFF690005)
    val errorContainerDark: Color = Color(0xFF93000A)
    val onErrorContainerDark: Color = Color(0xFFFFDAD6)

    // Background & Surface
    val backgroundLight: Color = Color(0xFFF6FBFF)
    val onBackgroundLight: Color = Color(0xFF172A41)
    val surfaceLight: Color = Color(0xFFFFFFFF)
    val onSurfaceLight: Color = Color(0xFF172A41)
    val surfaceVariantLight: Color = Color(0xFFEEF6FC)
    val onSurfaceVariantLight: Color = Color(0xFF576574)
    val surfaceContainerLowLight: Color = Color(0xFFF6FBFF)
    val surfaceContainerLight: Color = Color(0xFFEEF6FC)

    val backgroundDark: Color = Color(0xFF05101E)
    val onBackgroundDark: Color = Color(0xFFE4ECF2)
    val surfaceDark: Color = Color(0xFF0B192A)
    val onSurfaceDark: Color = Color(0xFFE4ECF2)
    val surfaceVariantDark: Color = Color(0xFF142234)
    val onSurfaceVariantDark: Color = Color(0xFF93A0AE)
    val surfaceContainerLowDark: Color = Color(0xFF05101E)
    val surfaceContainerDark: Color = Color(0xFF142234)

    // Outline / hairline borders
    val outlineLight: Color = Color(0xFFD9E3EA)
    val outlineVariantLight: Color = Color(0xFFE2EBF1)
    val outlineDark: Color = Color(0xFF293442)
    val outlineVariantDark: Color = Color(0xFF33404D)
}
