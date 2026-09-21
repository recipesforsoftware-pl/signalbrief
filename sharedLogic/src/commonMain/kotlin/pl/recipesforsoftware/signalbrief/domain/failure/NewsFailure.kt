package pl.recipesforsoftware.signalbrief.domain.failure

import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository

/**
 * Typed failures reported by [NewsRepository].
 *
 * Kept small and explicit so that UI and domain code switch on these types
 * instead of leaking raw Ktor or serialization exceptions. Cancellation is never
 * represented as a [NewsFailure]: it is always rethrown by the data layer.
 */
sealed class NewsFailure : Exception() {
    /** Transport-level failure: no connection, DNS, timeout, or HTTP error. */
    data object Network : NewsFailure()

    /** The remote payload could not be deserialized or mapped. */
    data object InvalidData : NewsFailure()

    /** Unexpected failure that could not be classified; carries the cause. */
    data class Unknown(
        override val cause: Throwable,
    ) : NewsFailure()
}
