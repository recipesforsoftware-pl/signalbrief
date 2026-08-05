package com.recipesforsoftware.mvvm.ui.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing scale used across SignalBrief screens.
 *
 * All values are in dp and chosen to create calm, breathable editorial layouts
 * without relying on platform-specific dimensions.
 */
object SignalBriefSpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 12.dp
    val l: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val xxxxl: Dp = 64.dp

    /** Standard horizontal page padding used by full-width screens. */
    val pageHorizontal: Dp = l

    /** Maximum content width for large screens; content stays centered beyond it. */
    val maxContentWidth: Dp = 600.dp
}
