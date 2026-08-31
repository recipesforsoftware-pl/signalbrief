package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefPresenter
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesPresenter
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchPresenter
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesPresenter
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

/** Reusable contract-only host for browser and other lightweight platform compositions. */
@Composable
fun SignalBriefAppHost(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
) {
    val savedArticles by savedArticlesRepository.observeAllSavedArticles().collectAsState(emptyList())
    val composition =
        remember(newsRepository, savedArticlesRepository) {
            PresentationComposition(newsRepository, savedArticlesRepository)
        }
    DisposableEffect(composition) { onDispose(composition::dispose) }
    SignalBriefApp(
        onboardingCompleted = true,
        onCompleteOnboarding = {},
        topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
            Headlines(composition.headlines, bottomBar, onArticleClick, onSearchClick)
        },
        savedContent = { bottomBar, onArticleClick -> Saved(composition.saved, bottomBar, onArticleClick) },
        dailyBriefContent = { bottomBar, onArticleClick -> Brief(composition.brief, bottomBar, onArticleClick) },
        searchContent = { initial, queryChanged, articleClick, back ->
            Search(composition::search, initial, queryChanged, articleClick, back)
        },
        articleDetailsContent = { article, back -> Details(article, savedArticlesRepository, back) },
        savedArticleCount = savedArticles.size,
    )
}

private class PresentationComposition(
    private val news: NewsRepository,
    private val savedRepository: SavedArticlesRepository,
) {
    val headlines = TopHeadlinesPresenter(news, savedRepository)
    val saved = SavedArticlesPresenter(savedRepository)
    val brief = DailyBriefPresenter(news, savedRepository)

    fun search(query: String) = SearchPresenter(news, savedRepository, query)

    fun dispose() {
        headlines.dispose()
        saved.dispose()
        brief.dispose()
    }
}

@Composable
private fun Headlines(
    p: TopHeadlinesPresenter,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
    search: () -> Unit,
) {
    val state by p.uiState.collectAsState()
    TopHeadlinesScreen(
        state,
        p::refresh,
        click,
        onBookmarkClick = p::toggleBookmark,
        onSearchClick = search,
        bottomBar = bottom,
    )
}

@Composable private fun Saved(
    p: SavedArticlesPresenter,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
) {
    val state by p.uiState.collectAsState()
    SavedArticlesScreen(state, click, { p.removeArticle(it.url) }, bottomBar = bottom)
}

@Composable private fun Brief(
    p: DailyBriefPresenter,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
) {
    val state by p.uiState.collectAsState()
    DailyBriefScreen(state, click, p::toggleBookmark, bottomBar = bottom)
}

@Composable
private fun Search(
    factory: (String) -> SearchPresenter,
    initial: String,
    changed: (String) -> Unit,
    click: (Article) -> Unit,
    back: () -> Unit,
) {
    val p = remember { factory(initial) }
    DisposableEffect(p) { onDispose(p::dispose) }
    val query by p.query.collectAsState()
    val state by p.uiState.collectAsState()
    SearchScreen(query, {
        p.setQuery(it)
        changed(it)
    }, state, click, p::toggleBookmark, back)
}

@Composable private fun Details(
    article: Article,
    repository: SavedArticlesRepository,
    back: () -> Unit,
) {
    val p = remember(article.url) { ArticleDetailsPresenter(repository, article) }
    DisposableEffect(p) { onDispose(p::dispose) }
    val state by p.uiState.collectAsState()
    val uri = LocalUriHandler.current
    ArticleDetailsScreen(
        uiState = state,
        onBack = back,
        onBookmarkClick = p::toggleBookmark,
        onOpenFullArticle = { if (article.hasActionableUrl()) uri.openUri(article.url) },
    )
}
