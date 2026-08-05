package com.recipesforsoftware.mvvm.ui.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shape scale for SignalBrief surfaces, cards, and buttons.
 *
 * Keeps corners subtle and consistent so the product feels modern without
 * becoming decorative.
 */
object SignalBriefShapes {
    val none = RoundedCornerShape(0.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val full = RoundedCornerShape(percent = 50)
}
