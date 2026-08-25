package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
internal actual fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val placeholderPainter = ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
    AsyncImage(
        model = imageReference,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = placeholderPainter,
        error = placeholderPainter,
        modifier = modifier,
    )
}
