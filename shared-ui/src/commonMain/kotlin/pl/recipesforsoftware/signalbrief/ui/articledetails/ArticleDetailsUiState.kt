package pl.recipesforsoftware.signalbrief.ui.articledetails

import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Framework-independent renderable state of the Article Details screen.
 *
 * Produced by [ArticleDetailsPresenter] and consumed by any Compose host
 * (Android and iOS). [article] is the locally available snapshot selected in
 * the originating destination; it is never refetched or enriched. [isSaved] is
 * derived from the persistence layer, never from a transient UI flag.
 */
data class ArticleDetailsUiState(
    val article: Article,
    val isSaved: Boolean = false,
)
