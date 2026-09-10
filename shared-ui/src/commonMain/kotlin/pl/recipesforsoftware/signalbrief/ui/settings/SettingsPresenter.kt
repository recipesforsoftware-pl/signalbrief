package pl.recipesforsoftware.signalbrief.ui.settings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
 * Framework-independent state holder for the Settings screen.
 *
 * Observes the locally cached top-headlines flow reactively to derive the
 * downloaded-headline count. Never calls [NewsRepository.getTopHeadlines];
 * the count is derived state only.
 *
 * Callers must call [dispose] when the screen is torn down so in-flight
 * collection is cancelled.
 */
class SettingsPresenter(
    private val newsRepository: NewsRepository,
    private val country: String = DEFAULT_NEWS_COUNTRY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeCachedArticles().collect { articles ->
                _uiState.value = SettingsUiState(downloadedHeadlineCount = articles.size)
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun observeCachedArticles(): Flow<List<Article>> = cachedArticles().catch { emit(emptyList()) }

    private fun cachedArticles(): Flow<List<Article>> = newsRepository.observeCachedTopHeadlines(country)
}
