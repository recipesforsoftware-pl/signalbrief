package pl.recipesforsoftware.signalbrief.ui.articledetails

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
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

/**
 * Framework-independent state holder for the Article Details screen.
 *
 * Owns the reactive bookmark observation for one selected [article]: it
 * collects [SavedArticlesRepository.isArticleSaved] for the article URL so the
 * bookmark state always mirrors persistence, and delegates save/remove to the
 * same repository. There is no optimistic local toggle: the icon only changes
 * when a persistence emission arrives, and a failed save/remove simply leaves
 * the state untouched.
 *
 * Depends only on the [SavedArticlesRepository] contract (never on concrete
 * implementations), owns its [CoroutineScope], and exposes immutable state via
 * [uiState]. Callers are responsible for calling [dispose] when the details
 * screen is torn down so that in-flight collection is cancelled.
 */
class ArticleDetailsPresenter(
    private val savedArticlesRepository: SavedArticlesRepository,
    private val article: Article,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow(ArticleDetailsUiState(article))
    val uiState: StateFlow<ArticleDetailsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            savedArticlesRepository.isArticleSaved(article.url).collect { isSaved ->
                _uiState.value = _uiState.value.copy(isSaved = isSaved)
            }
        }
    }

    /**
     * Toggles the bookmark state for the selected article. Delegates to
     * [SavedArticlesRepository] for persistence; the reactive saved-state
     * stream updates the UI automatically.
     */
    fun toggleBookmark() {
        if (!article.hasActionableUrl()) return
        scope.launch {
            if (_uiState.value.isSaved) {
                savedArticlesRepository.removeSavedArticle(article.url)
            } else {
                savedArticlesRepository.saveArticle(article)
            }
        }
    }

    /** Cancels the owned scope and all in-flight work. Safe to call multiple times. */
    fun dispose() {
        scope.cancel()
    }
}
