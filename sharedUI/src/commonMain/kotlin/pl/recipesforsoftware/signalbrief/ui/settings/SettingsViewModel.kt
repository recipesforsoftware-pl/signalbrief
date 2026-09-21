package pl.recipesforsoftware.signalbrief.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DEFAULT_NEWS_COUNTRY

/**
 * Lifecycle-aware state holder for the Settings screen.
 *
 * Observes the locally cached top-headlines flow reactively to derive the
 * downloaded-headline count. Never calls [NewsRepository.getTopHeadlines];
 * the count is derived state only.
 *
 * Its work is cancelled when its lifecycle owner clears this ViewModel.
 */
class SettingsViewModel(
    private val newsRepository: NewsRepository,
    private val country: String = DEFAULT_NEWS_COUNTRY,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCachedArticles().collect { articles ->
                _uiState.value = SettingsUiState(downloadedHeadlineCount = articles.size)
            }
        }
    }

    /**
     * Explicitly clears the locally downloaded Top Headlines cache for the
     * configured [country].
     *
     * The count is never forced here: it stays derived from
     * [NewsRepository.observeCachedTopHeadlines] and falls to zero only when the
     * local cache actually emits an empty list after the clear. A failed clear
     * leaves the count untouched and triggers no network refresh.
     */
    fun clearDownloadedHeadlines() {
        viewModelScope.launch {
            newsRepository.clearCachedTopHeadlines(country)
        }
    }

    private fun observeCachedArticles(): Flow<List<Article>> = cachedArticles().catch { emit(emptyList()) }

    private fun cachedArticles(): Flow<List<Article>> = newsRepository.observeCachedTopHeadlines(country)
}
