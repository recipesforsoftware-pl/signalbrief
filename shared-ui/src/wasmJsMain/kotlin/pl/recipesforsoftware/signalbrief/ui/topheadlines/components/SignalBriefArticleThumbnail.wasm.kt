@file:OptIn(ExperimentalWasmJsInterop::class)

package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
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

@Composable
internal actual fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val remoteBitmap =
        if (imageReference.startsWith(IMAGE_PROXY_PREFIX)) {
            val bitmap by produceState<ImageBitmap?>(
                initialValue = remoteImageCache[imageReference],
                key1 = imageReference,
            ) {
                if (value == null) {
                    value = loadRemoteImage(imageReference)
                    value?.let { loaded ->
                        remoteImageCache[imageReference] = loaded
                    }
                }
            }
            bitmap
        } else {
            null
        }

    val painter =
        remoteBitmap?.let(::BitmapPainter)
            ?: ColorPainter(MaterialTheme.colorScheme.surfaceContainer)

    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

private suspend fun loadRemoteImage(imageReference: String): ImageBitmap? =
    runCatching {
        val response =
            window
                .fetch(imageReference)
                .awaitValue()

        check(response.ok)

        val arrayBuffer =
            response
                .arrayBuffer()
                .awaitValue()

        Int8Array(arrayBuffer)
            .toByteArray()
            .decodeToImageBitmap()
    }.fold(
        onSuccess = { bitmap ->
            bitmap
        },
        onFailure = { failure ->
            if (failure is CancellationException) {
                throw failure
            }
            null
        },
    )

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
