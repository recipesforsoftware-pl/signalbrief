@file:OptIn(ExperimentalWasmJsInterop::class)

package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.asJsException

private const val IMAGE_PROXY_PREFIX = "/api/image?"

private val remoteImageCache = mutableMapOf<String, ImageBitmap>()

internal sealed interface RemoteImageLoadState {
    data object Loading : RemoteImageLoadState

    data class Success(
        val bitmap: ImageBitmap,
    ) : RemoteImageLoadState

    data object Failure : RemoteImageLoadState
}

@Composable
internal actual fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    if (!imageReference.startsWith(IMAGE_PROXY_PREFIX)) {
        SignalBriefImageFallbackSurface(modifier)
        return
    }

    val imageState by
        produceState<RemoteImageLoadState>(
            initialValue = RemoteImageLoadState.Loading,
            key1 = imageReference,
        ) {
            val cachedBitmap = remoteImageCache[imageReference]
            if (cachedBitmap != null) {
                value = RemoteImageLoadState.Success(cachedBitmap)
                return@produceState
            }

            value = RemoteImageLoadState.Loading
            try {
                val loaded = loadRemoteImage(imageReference)
                remoteImageCache[imageReference] = loaded
                value = RemoteImageLoadState.Success(loaded)
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Throwable) {
                value = RemoteImageLoadState.Failure
            }
        }

    when (val state = imageState) {
        RemoteImageLoadState.Loading -> {
            SignalBriefImageLoadingPlaceholder(modifier)
        }

        is RemoteImageLoadState.Success -> {
            Image(
                painter = BitmapPainter(state.bitmap),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
        }

        RemoteImageLoadState.Failure -> {
            SignalBriefImageFallbackSurface(modifier)
        }
    }
}

private suspend fun loadRemoteImage(imageReference: String): ImageBitmap {
    val response =
        window
            .fetch(imageReference)
            .awaitValue()

    check(response.ok)

    val arrayBuffer =
        response
            .arrayBuffer()
            .awaitValue()

    return Int8Array(arrayBuffer)
        .toByteArray()
        .decodeToImageBitmap()
}

private suspend fun <T : JsAny?> Promise<T>.awaitValue(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resumeWith(Result.success(value))
                null
            },
            onRejected = { reason ->
                continuation.resumeWithException(reason.asJsException())
                null
            },
        )
    }
