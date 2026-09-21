package pl.recipesforsoftware.signalbrief.ui.articledetails

import pl.recipesforsoftware.signalbrief.domain.model.Collection

/** Immutable renderable state for the Article Details collection picker. */
data class ArticleCollectionAssignmentUiState(
    val collections: List<Collection> = emptyList(),
    val selectedCollectionIds: Set<String> = emptySet(),
    val isLoadingCollections: Boolean = true,
    val isPickerVisible: Boolean = false,
    val mutatingCollectionIds: Set<String> = emptySet(),
    val error: ArticleCollectionAssignmentError? = null,
)

enum class ArticleCollectionAssignmentError { NotFound, Unknown }
