package com.recipesforsoftware.mvvm.ui.topheadlines

import com.recipesforsoftware.mvvm.domain.model.Article

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

    /** Headlines were loaded successfully. */
    data class Success(
        val articles: List<Article>,
    ) : TopHeadlinesUiState

    /** The feed is available but returned no articles. */
    data object Empty : TopHeadlinesUiState

    /** The last fetch failed with a typed, user-presentable reason. */
    data class Error(
        val error: TopHeadlinesError,
    ) : TopHeadlinesUiState
}
