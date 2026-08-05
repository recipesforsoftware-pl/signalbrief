package com.recipesforsoftware.mvvm.ui.images

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory

/**
 * Single source of truth for the shared Coil [ImageLoader].
 *
 * Installed once at the app composition root (for example [SignalBriefApp]) so
 * the singleton is configured before any image is requested. Article thumbnails
 * load through Coil's Ktor 3 network fetcher, which removes the previous OkHttp
 * dependency; it does **not** automatically share the exact [HttpClient]
 * instance owned by the news repository (i.e. [io.ktor.client.HttpClient]) unless one is
 * explicitly injected.
 */
@Composable
fun installSignalBriefImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }.build()
    }
}
