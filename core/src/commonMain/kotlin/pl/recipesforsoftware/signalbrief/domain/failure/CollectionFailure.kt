package pl.recipesforsoftware.signalbrief.domain.failure

import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

/**
 * Typed failures reported by [CollectionsRepository].
 *
 * Kept small and explicit so that domain and UI code switch on these types
 * instead of leaking database or persistence exceptions.
 */
sealed class CollectionFailure : Throwable() {
    /** The supplied collection name is blank after trimming and was rejected. */
    data object InvalidName : CollectionFailure()

    /** No collection exists with the supplied id. */
    data object NotFound : CollectionFailure()
}
