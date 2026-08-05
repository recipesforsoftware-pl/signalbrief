package com.recipesforsoftware.mvvm.ui.topheadlines.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefShapes

/**
 * Decorative placeholder that mirrors the geometry of [ArticleCard] so the
 * loading layout does not jump when real content arrives. A gentle opacity
 * pulse on standard Compose APIs replaces a heavy shimmer library.
 *
 * Intentionally exposes no semantics: the enclosing loading container
 * announces a single description for the whole loading state, so screen
 * readers never iterate over these placeholders.
 */
@Composable
fun SkeletonArticleCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 900),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "skeletonAlpha",
        )
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SkeletonLine(widthFraction = 0.4f, color = placeholderColor)
            SkeletonLine(widthFraction = 0.9f, color = placeholderColor)
            SkeletonLine(widthFraction = 0.7f, color = placeholderColor)
        }
        Box(
            modifier =
                Modifier
                    .size(88.dp, 72.dp)
                    .clip(SignalBriefShapes.small)
                    .background(placeholderColor),
        )
    }
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .height(12.dp)
                .clip(SignalBriefShapes.small)
                .background(color),
    )
}
