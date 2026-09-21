package pl.recipesforsoftware.signalbrief.ui.collectiondetails

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection

/** Renderable state for one collection's independently persisted article snapshots. */
data class CollectionDetailsUiState(
    val collection: Collection,
    val articles: List<Article> = emptyList(),
)
