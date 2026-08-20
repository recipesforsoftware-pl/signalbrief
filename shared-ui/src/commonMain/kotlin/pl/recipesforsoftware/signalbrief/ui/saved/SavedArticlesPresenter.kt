package pl.recipesforsoftware.signalbrief.ui.saved

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

/**
 * Framework-independent state holder for the Saved Articles screen.
 *
 * Depends only on [SavedArticlesRepository] (never on concrete implementations),
 * owns its [CoroutineScope], and exposes immutable state via [uiState]. Callers
 * are responsible for calling [dispose] when the screen is torn down so that
 * in-flight collection is cancelled.
 *
 * The presenter collects a single [SavedArticlesRepository.observeAllSavedArticles]
 * stream per lifecycle. No per-card queries, no polling, no second in-memory
 * source of truth.
 */
class SavedArticlesPresenter(
    private val savedArticlesRepository: SavedArticlesRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow<SavedArticlesUiState>(SavedArticlesUiState.Loading)
    val uiState: StateFlow<SavedArticlesUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            savedArticlesRepository.observeAllSavedArticles().collect { articles ->
                _uiState.value =
                    if (articles.isEmpty()) {
                        SavedArticlesUiState.Empty
                    } else {
                        SavedArticlesUiState.Content(articles)
                    }
            }
        }
    }

    /**
     * Removes the saved article with the given [url]. Delegates to
     * [SavedArticlesRepository] for persistence; the reactive stream updates
     * the UI automatically.
     */
    fun removeArticle(url: String) {
        scope.launch {
            savedArticlesRepository.removeSavedArticle(url)
        }
    }

    /** Cancels the owned scope and all in-flight work. Safe to call multiple times. */
    fun dispose() {
        scope.cancel()
    }
}
