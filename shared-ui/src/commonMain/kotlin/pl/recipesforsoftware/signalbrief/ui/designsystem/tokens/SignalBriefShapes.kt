package pl.recipesforsoftware.signalbrief.ui.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shape scale for SignalBrief surfaces, cards, and buttons.
 *
 * Mapped from the prototype radius tokens (`ui/shared/tokens.css`):
 * - radius-sm: 10px
 * - radius: 14px
 * - radius-lg: 20px
 *
 * Keeps corners subtle and consistent so the product feels modern without
 * becoming decorative.
 */
object SignalBriefShapes {
    val none = RoundedCornerShape(0.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(14.dp)
    val extraLarge = RoundedCornerShape(20.dp)
    val full = RoundedCornerShape(percent = 50)
}
