package pl.recipesforsoftware.signalbrief.ui.search

import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Framework-independent renderable state of the Local Search screen.
 *
 * Produced by [SearchPresenter] and consumed by any Compose host (Android and
 * iOS). Search operates only on locally cached headlines; there is no network
 * branch and no remote error state.
 */
sealed interface SearchUiState {
    /** Initial state before the first local-cache emission arrives. */
    data object Loading : SearchUiState

    /** Cached headlines exist and the query is blank; waiting for input. */
    data object Idle : SearchUiState

    /** Non-blank query with at least one matching cached headline. */
    data class Results(
        val query: String,
        val articles: List<Article>,
        val savedUrls: Set<String>,
    ) : SearchUiState

    /** Non-blank query with no matches in the local cache. */
    data class NoResults(
        val query: String,
    ) : SearchUiState

    /** The local cache contains no headlines at all. */
    data object NoLocalArticles : SearchUiState
}
