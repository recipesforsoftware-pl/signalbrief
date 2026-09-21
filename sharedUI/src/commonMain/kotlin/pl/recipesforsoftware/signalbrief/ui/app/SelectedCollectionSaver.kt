package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.recipesforsoftware.signalbrief.domain.model.Collection

/** Compose [Saver] for the selected Collection Details destination. */
internal val SelectedCollectionSaver: Saver<Collection?, Any> =
    listSaver(
        save = { collection -> collection?.let { listOf(it.id, it.name) }.orEmpty() },
        restore = { values ->
            values.takeIf { it.size == 2 }?.let {
                Collection(id = it[0], name = it[1])
            }
        },
    )
