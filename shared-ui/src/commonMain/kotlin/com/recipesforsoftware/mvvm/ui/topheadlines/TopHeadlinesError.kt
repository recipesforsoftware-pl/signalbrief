package com.recipesforsoftware.mvvm.ui.topheadlines

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import kotlin.coroutines.cancellation.CancellationException

/**
 * Presentation-level classification of a [NewsFailure].
 *
 * Kept as stable identifiers so the presenter stays framework-independent and
 * the UI can map them to localized user messages through
 * [TopHeadlinesStrings]. Raw exception messages are never exposed to users.
 */
enum class TopHeadlinesError {
    /** Transport-level failure: no connection, timeout, DNS, or HTTP error. */
    Network,

    /** The remote payload could not be deserialized or mapped. */
    InvalidData,

    /** Unexpected failure that could not be classified. */
    Unknown,
}

/**
 * Maps a domain [NewsFailure] into a presentation-level [TopHeadlinesError].
 *
 * Cancellation is never a user-facing error: it is always rethrown so that the
 * calling coroutine observes structured concurrency. Any other throwable that
 * is not a [NewsFailure] collapses to [TopHeadlinesError.Unknown].
 */
internal fun Throwable.toTopHeadlinesError(): TopHeadlinesError {
    if (this is CancellationException) {
        throw this
    }
    return when (this) {
        is NewsFailure.Network -> TopHeadlinesError.Network
        is NewsFailure.InvalidData -> TopHeadlinesError.InvalidData
        is NewsFailure.Unknown -> TopHeadlinesError.Unknown
        else -> TopHeadlinesError.Unknown
    }
}
