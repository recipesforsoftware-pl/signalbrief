package com.recipesforsoftware.mvvm.ui.topheadlines

/**
 * Centralized user-facing strings of the Top Headlines screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth. Error bodies
 * are keyed by [TopHeadlinesError] so presentation never leaks raw exceptions.
 */
object TopHeadlinesStrings {
    const val TOP_BAR_TITLE: String = "Top Headlines"
    const val TOP_BAR_SUBTITLE: String = "Latest news from around the world"

    const val REFRESH: String = "Refresh"
    const val RETRY: String = "Try Again"

    const val LOADING: String = "Loading headlines..."

    const val EMPTY_TITLE: String = "No headlines right now"
    const val EMPTY_SUBTITLE: String = "Check back later or use the refresh button to try again."

    const val ERROR_TITLE: String = "Something went wrong"

    /** Maps a presentation-level [TopHeadlinesError] to a user-facing message. */
    fun errorBody(error: TopHeadlinesError): String =
        when (error) {
            TopHeadlinesError.Network -> "Network error. Please check your connection and try again."
            TopHeadlinesError.InvalidData -> "Invalid data received from the server."
            TopHeadlinesError.Unknown -> "An unexpected error occurred."
        }

    /** Returns a human-readable [errorBody] for any [TopHeadlinesUiState] if it is an error state. */
    fun errorBody(uiState: TopHeadlinesUiState): String? =
        when (uiState) {
            is TopHeadlinesUiState.Error -> errorBody(uiState.error)
            else -> null
        }
}
