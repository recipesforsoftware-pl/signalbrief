package pl.recipesforsoftware.signalbrief.ui.topheadlines

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource

/**
 * Framework-independent renderable state of the Top Headlines screen.
 *
 * Produced by [TopHeadlinesPresenter] and consumed by any Compose host
 * (Android and iOS). The error branch carries a presentation-level message
 * identifier rather than a raw exception so that hosts never render
 * implementation details.
 */
sealed interface TopHeadlinesUiState {
    /** Initial fetch is in flight and no content is available yet. */
    data object Loading : TopHeadlinesUiState

    /** Headlines were loaded successfully, tagged with where they came from. */
    data class Success(
        val articles: List<Article>,
        val source: FeedSource,
        val savedUrls: Set<String> = emptySet(),
    ) : TopHeadlinesUiState

    /** The feed is available but returned no articles. */
    data object Empty : TopHeadlinesUiState

    /** The last fetch failed with a typed, user-presentable reason. */
    data class Error(
        val error: TopHeadlinesError,
    ) : TopHeadlinesUiState
}
