package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

/**
 * Framework-independent renderable state of the Topic Matches screen.
 *
 * Produced by [TopicMatchesPresenter] and consumed by any Compose host. Matches
 * are derived entirely from the locally cached headlines and the selected,
 * already-persisted [MonitoredTopic]; there is no network branch and no remote
 * error state.
 */
sealed interface TopicMatchesUiState {
    /** Initial state before the first local-cache emission arrives. */
    data class Loading(
        val topic: MonitoredTopic,
    ) : TopicMatchesUiState

    /** The local headline cache is empty, so no topic can match. */
    data class NoLocalArticles(
        val topic: MonitoredTopic,
    ) : TopicMatchesUiState

    /** Cached headlines exist, but none match the selected topic. */
    data class NoMatches(
        val topic: MonitoredTopic,
    ) : TopicMatchesUiState

    /** At least one cached headline matches the selected topic. */
    data class Content(
        val topic: MonitoredTopic,
        val articles: List<Article>,
        val savedUrls: Set<String>,
    ) : TopicMatchesUiState
}
