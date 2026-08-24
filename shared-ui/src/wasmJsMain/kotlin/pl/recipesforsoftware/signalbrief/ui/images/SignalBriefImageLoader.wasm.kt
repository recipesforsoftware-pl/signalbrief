package pl.recipesforsoftware.signalbrief.ui.images

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory

@Composable
actual fun installSignalBriefImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).build()
    }
}
