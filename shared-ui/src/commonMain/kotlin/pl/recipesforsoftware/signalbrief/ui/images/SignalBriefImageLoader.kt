package pl.recipesforsoftware.signalbrief.ui.images

import androidx.compose.runtime.Composable

/**
 * Single source of truth for the shared Coil [ImageLoader].
 *
 * Installed once at the app composition root (for example [SignalBriefApp]) so the
 * singleton is configured before any image is requested. Native platforms install Coil's
 * Ktor network fetcher; Wasm intentionally uses Coil's plain loader because the Web demo
 * only supplies null image URLs and its fallback path does not fetch images.
 */
@Composable
expect fun installSignalBriefImageLoader()
