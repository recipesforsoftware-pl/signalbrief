package pl.recipesforsoftware.signalbrief.ui.saved

import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Framework-independent renderable state of the Saved Articles screen.
 *
 * Produced by [SavedArticlesPresenter] and consumed by any Compose host
 * (Android and iOS). The state is derived directly from the Room-backed
 * [SavedArticlesRepository] reactive stream; no artificial loading or
 * network-error states exist because persistence emissions are immediate.
 */
sealed interface SavedArticlesUiState {
    /** Initial state before the first Room emission arrives. */
    data object Loading : SavedArticlesUiState

    /** Saved articles are available, ordered most-recently-saved first. */
    data class Content(
        val articles: List<Article>,
    ) : SavedArticlesUiState

    /** The user has no saved articles. */
    data object Empty : SavedArticlesUiState
}
