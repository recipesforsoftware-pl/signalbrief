package pl.recipesforsoftware.signalbrief.ui.designsystem.components

import androidx.compose.runtime.Stable

/**
 * Sigby artwork variant.
 *
 * [Full] is the larger, emotional-state artwork used for onboarding, empty
 * states, and success moments. [Compact] is the smaller, square-ish mark used for
 * banners, errors, and inline empty states.
 */
@Stable
enum class SigbyVariant {
    Full,
    Compact,
}
