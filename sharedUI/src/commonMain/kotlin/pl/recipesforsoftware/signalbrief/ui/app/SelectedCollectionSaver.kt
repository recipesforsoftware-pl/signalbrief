package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.recipesforsoftware.signalbrief.domain.model.Collection

/** KMP-safe [Saver] for Navigation 3 Collection Details rendering payloads. */
internal val NavigationCollectionSnapshotsSaver: Saver<Map<String, Collection>, Any> =
    listSaver(
        save = { snapshots -> snapshots.values.flatMap(Collection::toSavedValues) },
        restore = { values ->
            values
                .takeIf { it.size % COLLECTION_SAVED_VALUE_COUNT == 0 }
                ?.chunked(COLLECTION_SAVED_VALUE_COUNT)
                ?.mapNotNull { it.toCollection() }
                ?.associateBy(Collection::id)
                .orEmpty()
        },
    )

private fun Collection.toSavedValues(): List<Any?> = listOf(id, name)

private fun List<Any?>.toCollection(): Collection? =
    runCatching {
        Collection(id = this[0] as String, name = this[1] as String)
    }.getOrNull()

private const val COLLECTION_SAVED_VALUE_COUNT = 2
