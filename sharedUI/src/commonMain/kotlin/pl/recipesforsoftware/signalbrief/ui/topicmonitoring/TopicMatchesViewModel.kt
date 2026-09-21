package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

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
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.usecase.ArticleQueryMatcher
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DEFAULT_NEWS_COUNTRY
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

/**
 * Framework-independent state holder for the Topic Matches screen.
 *
 * Combines the locally cached top headlines (via
 * [NewsRepository.observeCachedTopHeadlines]) with the reactive saved-article
 * stream and the selected [MonitoredTopic]. Matches are derived in memory using
 * the shared [ArticleQueryMatcher]; no remote search request is ever issued and
 * no network refresh is performed.
 *
 * Any cached-headline observation failure is treated
 * as an empty local cache, consistent with the Local Search convention.
 */
class TopicMatchesViewModel(
    private val topic: MonitoredTopic,
    private val newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val matcher: ArticleQueryMatcher = ArticleQueryMatcher,
    private val country: String = DEFAULT_NEWS_COUNTRY,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopicMatchesUiState>(TopicMatchesUiState.Loading(topic))
    val uiState: StateFlow<TopicMatchesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeCachedArticles(),
                savedArticlesRepository.observeAllSavedArticles(),
                ::buildUiState,
            ).collect { _uiState.value = it }
        }
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

    private fun observeCachedArticles(): Flow<List<Article>> =
        newsRepository
            .observeCachedTopHeadlines(country)
            .catch { emit(emptyList()) }

    private fun buildUiState(
        articles: List<Article>,
        savedArticles: List<Article>,
    ): TopicMatchesUiState {
        val savedUrls = savedArticles.mapTo(HashSet(savedArticles.size)) { it.url }
        val query = topic.query
        return when {
            articles.isEmpty() -> {
                TopicMatchesUiState.NoLocalArticles(topic)
            }

            else -> {
                val matches = matcher.filter(articles, query)
                if (matches.isEmpty()) {
                    TopicMatchesUiState.NoMatches(topic)
                } else {
                    TopicMatchesUiState.Content(topic, matches, savedUrls)
                }
            }
        }
    }
}
