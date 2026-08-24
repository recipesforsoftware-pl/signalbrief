package pl.recipesforsoftware.signalbrief.ui.search

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DEFAULT_NEWS_COUNTRY
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

/**
 * Framework-independent state holder for the Local Search screen.
 *
 * Depends on [NewsRepository] for the locally cached headline stream and on
 * [SavedArticlesRepository] for reactive bookmark state. Search is performed
 * entirely in memory over the cached articles; no remote NewsAPI request is
 * ever issued.
 *
 * The presenter owns its [CoroutineScope], the query state, and an immutable
 * [uiState] [StateFlow]. Callers must call [dispose] when the screen is torn
 * down so collection is cancelled.
 */
class SearchPresenter(
    private val newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    initialQuery: String = "",
    private val country: String = DEFAULT_NEWS_COUNTRY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                observeCachedArticles(),
                savedArticlesRepository.observeAllSavedArticles(),
                _query,
                ::buildUiState,
            ).collect { _uiState.value = it }
        }
    }

    /**
     * Updates the search query. Matching is performed after trimming leading and
     * trailing whitespace.
     */
    fun setQuery(value: String) {
        _query.value = value
    }

    /**
     * Toggles the bookmark state for [article]. Delegates to
     * [SavedArticlesRepository] for persistence; the reactive saved-URL stream
     * updates the UI automatically.
     */
    fun toggleBookmark(article: Article) {
        if (!article.hasActionableUrl()) return
        scope.launch {
            val saved = savedArticlesRepository.observeAllSavedArticles().first()
            val isSaved = saved.any { it.url == article.url }
            if (isSaved) {
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

    private fun observeCachedArticles(): Flow<List<Article>> = cachedArticles().catch { emit(emptyList()) }

    private fun cachedArticles(): Flow<List<Article>> = newsRepository.observeCachedTopHeadlines(country)

    private fun buildUiState(
        articles: List<Article>,
        savedArticles: List<Article>,
        query: String,
    ): SearchUiState {
        val trimmed = query.trim()
        return when {
            articles.isEmpty() -> {
                SearchUiState.NoLocalArticles
            }

            trimmed.isBlank() -> {
                SearchUiState.Idle
            }

            else -> {
                val savedUrls = savedArticles.mapTo(HashSet(savedArticles.size)) { it.url }
                val matches = articles.filter { it.matches(trimmed) }
                if (matches.isEmpty()) {
                    SearchUiState.NoResults(trimmed)
                } else {
                    SearchUiState.Results(trimmed, matches, savedUrls)
                }
            }
        }
    }
}

private fun Article.matches(query: String): Boolean {
    val lowerQuery = query.lowercase()
    return title?.lowercase()?.contains(lowerQuery) == true ||
        description?.lowercase()?.contains(lowerQuery) == true ||
        source?.name?.lowercase()?.contains(lowerQuery) == true
}
