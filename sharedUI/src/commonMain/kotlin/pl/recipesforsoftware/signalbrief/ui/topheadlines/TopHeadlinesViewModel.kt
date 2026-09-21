package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

/** Default country used by the Top Headlines feed when the host does not override it. */
const val DEFAULT_NEWS_COUNTRY: String = "us"

/**
 * Lifecycle-aware state holder for the Top Headlines screen.
 *
 * Depends on both the [NewsRepository] and [SavedArticlesRepository]
 * contracts (never on concrete implementations) and exposes immutable state
 * via [uiState].
 *
 * Bookmark state is derived from a single [SavedArticlesRepository] stream
 * rather than one database query per feed card. The saved-URL set is collected
 * once and combined with the feed state so that bookmark UI updates
 * reactively when persistence changes.
 *
 * Concurrency model: a monotonically increasing request generation guards
 * against a stale response overwriting a newer one when concurrent refreshes
 * are issued.
 */
class TopHeadlinesViewModel(
    private val repository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val country: String = DEFAULT_NEWS_COUNTRY,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopHeadlinesUiState>(TopHeadlinesUiState.Loading)
    val uiState: StateFlow<TopHeadlinesUiState> = _uiState.asStateFlow()

    private val savedUrlCache = MutableStateFlow<Set<String>>(emptySet())

    private var requestGeneration = 0L

    init {
        viewModelScope.launch {
            savedArticlesRepository.observeAllSavedArticles().collect { articles ->
                val urls = articles.mapTo(mutableSetOf()) { it.url }
                savedUrlCache.value = urls
                applySavedUrlsToCurrentState()
            }
        }
        refresh()
    }

    /** Loads (or reloads) the top headlines, showing [TopHeadlinesUiState.Loading] while in flight. */
    fun refresh() {
        val generation = ++requestGeneration
        viewModelScope.launch {
            _uiState.value = TopHeadlinesUiState.Loading
            val result = repository.getTopHeadlines(country)
            if (generation != requestGeneration) {
                return@launch
            }
            result.fold(
                onSuccess = { feed ->
                    _uiState.value =
                        if (feed.articles.isEmpty()) {
                            TopHeadlinesUiState.Empty
                        } else {
                            TopHeadlinesUiState.Success(
                                feed.articles,
                                feed.source,
                                savedUrls = savedUrlCache.value,
                            )
                        }
                },
                onFailure = { failure ->
                    _uiState.value = TopHeadlinesUiState.Error(failure.toTopHeadlinesError())
                },
            )
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
            val isSaved = savedUrlCache.value.contains(article.url)
            if (isSaved) {
                savedArticlesRepository.removeSavedArticle(article.url)
            } else {
                savedArticlesRepository.saveArticle(article)
            }
        }
    }

    private fun applySavedUrlsToCurrentState() {
        val current = _uiState.value
        if (current is TopHeadlinesUiState.Success) {
            _uiState.value = current.copy(savedUrls = savedUrlCache.value)
        }
    }
}
