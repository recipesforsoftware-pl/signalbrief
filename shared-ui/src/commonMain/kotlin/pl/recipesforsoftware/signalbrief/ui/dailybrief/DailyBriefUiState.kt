package pl.recipesforsoftware.signalbrief.ui.dailybrief

import pl.recipesforsoftware.signalbrief.domain.model.Article

sealed interface DailyBriefUiState {
    data object Loading : DailyBriefUiState

    data object Empty : DailyBriefUiState

    data class Content(
        val articles: List<Article>,
        val savedUrls: Set<String>,
    ) : DailyBriefUiState
}
