package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp

/** Decorative, bounds-preserving image placeholder used while an article image is loading. */
@Composable
internal fun SignalBriefImageLoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
        )
    }
}

/** Stable neutral surface for absent or failed article images. */
@Composable
internal fun SignalBriefImageFallbackSurface(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
internal fun SignalBriefImageFallbackPainter(): ColorPainter = ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
