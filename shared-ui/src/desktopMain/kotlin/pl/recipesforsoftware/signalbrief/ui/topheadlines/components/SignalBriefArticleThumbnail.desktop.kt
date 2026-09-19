package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
internal actual fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    var isLoading by remember(imageReference) { mutableStateOf(false) }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageReference,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            placeholder = SignalBriefImageFallbackPainter(),
            error = SignalBriefImageFallbackPainter(),
            onLoading = { isLoading = true },
            onSuccess = { isLoading = false },
            onError = { isLoading = false },
        )

        if (isLoading) {
            SignalBriefImageLoadingPlaceholder()
        }
    }
}
