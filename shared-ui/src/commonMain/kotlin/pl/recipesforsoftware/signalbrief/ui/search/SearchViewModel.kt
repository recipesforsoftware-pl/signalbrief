package pl.recipesforsoftware.signalbrief.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import pl.recipesforsoftware.signalbrief.domain.usecase.ArticleQueryMatcher
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
 * Owns the query state and an immutable [uiState] [StateFlow].
 */
class SearchViewModel(
    private val newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    initialQuery: String = "",
    private val country: String = DEFAULT_NEWS_COUNTRY,
) : ViewModel() {
    private val _query = MutableStateFlow(initialQuery)
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val saved = savedArticlesRepository.observeAllSavedArticles().first()
            val isSaved = saved.any { it.url == article.url }
            if (isSaved) {
                savedArticlesRepository.removeSavedArticle(article.url)
            } else {
                savedArticlesRepository.saveArticle(article)
            }
        }
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
                val matches = ArticleQueryMatcher.filter(articles, trimmed)
                if (matches.isEmpty()) {
                    SearchUiState.NoResults(trimmed)
                } else {
                    SearchUiState.Results(trimmed, matches, savedUrls)
                }
            }
        }
    }
}
