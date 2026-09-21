package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** Platform boundary for article thumbnails: Web demo resources are rendered without Coil. */
@Composable
internal expect fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
)
