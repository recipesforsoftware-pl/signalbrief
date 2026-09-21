package pl.recipesforsoftware.signalbrief.ui.collections

import pl.recipesforsoftware.signalbrief.domain.model.Collection

/** Immutable, renderable state for collection management. */
data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val editor: CollectionsEditor? = null,
    val collectionPendingDeletion: Collection? = null,
    val error: CollectionsError? = null,
    val isSubmitting: Boolean = false,
)

sealed interface CollectionsEditor {
    val name: String

    data class Create(
        override val name: String = "",
    ) : CollectionsEditor

    data class Rename(
        val collection: Collection,
        override val name: String = collection.name,
    ) : CollectionsEditor
}

enum class CollectionsError { InvalidName, NotFound, Unknown }
