package pl.recipesforsoftware.signalbrief.ui.dailybrief

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.usecase.DailyBriefSelector
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DEFAULT_NEWS_COUNTRY
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

/** A local-only, reactive state holder for the Daily Brief reader. */
class DailyBriefPresenter(
    private val newsRepository: NewsRepository,
    private val savedArticlesRepository: SavedArticlesRepository,
    private val selector: DailyBriefSelector = DailyBriefSelector(),
    private val country: String = DEFAULT_NEWS_COUNTRY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow<DailyBriefUiState>(DailyBriefUiState.Loading)
    val uiState: StateFlow<DailyBriefUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                newsRepository.observeCachedTopHeadlines(country),
                savedArticlesRepository.observeAllSavedArticles(),
            ) { cached, saved ->
                val articles = selector(cached).articles
                if (articles.isEmpty()) {
                    DailyBriefUiState.Empty
                } else {
                    DailyBriefUiState.Content(articles, saved.mapTo(mutableSetOf()) { it.url })
                }
            }.collect { _uiState.value = it }
        }
    }

    fun toggleBookmark(article: Article) {
        if (!article.hasActionableUrl()) return
        scope.launch {
            val savedUrls = (_uiState.value as? DailyBriefUiState.Content)?.savedUrls.orEmpty()
            if (article.url in savedUrls) {
                savedArticlesRepository.removeSavedArticle(article.url)
            } else {
                savedArticlesRepository.saveArticle(article)
            }
        }
    }

    fun dispose() = scope.cancel()
}
