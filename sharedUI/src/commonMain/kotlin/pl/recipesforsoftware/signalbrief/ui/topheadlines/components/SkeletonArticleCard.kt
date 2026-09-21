package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefShapes

/**
 * Decorative placeholder that mirrors the geometry of [ArticleCard] so the
 * loading layout does not jump when real content arrives. It renders inside the
 * same card-shaped, hairline-bordered container as the real card. A gentle
 * opacity pulse on standard Compose APIs replaces a heavy shimmer library.
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
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SignalBriefShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonLine(widthFraction = 0.3f, height = 12.dp, color = placeholderColor)
                SkeletonLine(widthFraction = 0.95f, height = 20.dp, color = placeholderColor)
                SkeletonLine(widthFraction = 0.85f, height = 20.dp, color = placeholderColor)
                SkeletonLine(widthFraction = 0.6f, height = 20.dp, color = placeholderColor)
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
}

@Composable
private fun SkeletonLine(
    widthFraction: Float,
    height: Dp,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .height(height)
                .clip(SignalBriefShapes.small)
                .background(color),
    )
}
