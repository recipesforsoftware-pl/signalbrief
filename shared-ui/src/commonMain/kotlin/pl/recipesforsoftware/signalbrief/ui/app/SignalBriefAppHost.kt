package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsPresenter
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsPresenter
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefPresenter
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesPresenter
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchPresenter
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesPresenter
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesPresenter
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringPresenter
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen

/** Reusable contract-only host for browser and other lightweight platform compositions. */
@Composable
fun SignalBriefAppHost(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    topicMonitoringRepository: TopicMonitoringRepository,
) {
    val savedArticles by savedArticlesRepository.observeAllSavedArticles().collectAsState(emptyList())
    val composition =
        remember(newsRepository, savedArticlesRepository, collectionsRepository, topicMonitoringRepository) {
            PresentationComposition(
                newsRepository,
                savedArticlesRepository,
                collectionsRepository,
                topicMonitoringRepository,
            )
        }
    DisposableEffect(composition) { onDispose(composition::dispose) }
    SignalBriefApp(
        onboardingCompleted = true,
        onCompleteOnboarding = {},
        topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
            Headlines(composition.headlines, bottomBar, onArticleClick, onSearchClick)
        },
        savedContent = { bottomBar, onArticleClick, onCollectionsClick ->
            Saved(composition.saved, bottomBar, onArticleClick, onCollectionsClick)
        },
        dailyBriefContent = { bottomBar, onArticleClick -> Brief(composition.brief, bottomBar, onArticleClick) },
        searchContent = { initial, queryChanged, articleClick, monitoring, back ->
            Search(composition::search, initial, queryChanged, articleClick, monitoring, back)
        },
        articleDetailsContent = { article, back, collections ->
            Details(article, savedArticlesRepository, collectionsRepository, back, collections)
        },
        collectionsContent = { back, open -> Collections(composition.collections, back, open) },
        collectionDetailsContent = { collection, articleClick, back ->
            CollectionDetails(collection, collectionsRepository, articleClick, back)
        },
        topicMonitoringContent = { openMatches, back ->
            TopicMonitoring(composition.topicMonitoring, openMatches, back)
        },
        topicMatchesContent = { topic, articleClick, back ->
            TopicMatches(composition::topicMatches, topic, articleClick, back)
        },
        savedArticleCount = savedArticles.size,
    )
}

private class PresentationComposition(
    private val news: NewsRepository,
    private val savedRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    topicMonitoringRepository: TopicMonitoringRepository,
) {
    val headlines = TopHeadlinesPresenter(news, savedRepository)
    val saved = SavedArticlesPresenter(savedRepository)
    val brief = DailyBriefPresenter(news, savedRepository)
    val collections = CollectionsPresenter(collectionsRepository)
    val topicMonitoring = TopicMonitoringPresenter(topicMonitoringRepository, news)

    fun search(query: String) = SearchPresenter(news, savedRepository, query)

    fun topicMatches(topic: MonitoredTopic) = TopicMatchesPresenter(topic, news, savedRepository)

    fun dispose() {
        headlines.dispose()
        saved.dispose()
        brief.dispose()
        collections.dispose()
        topicMonitoring.dispose()
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
    collections: () -> Unit,
) {
    val state by p.uiState.collectAsState()
    SavedArticlesScreen(state, click, { p.removeArticle(it.url) }, collections, bottomBar = bottom)
}

@Composable
private fun Collections(
    p: CollectionsPresenter,
    back: () -> Unit,
    open: (pl.recipesforsoftware.signalbrief.domain.model.Collection) -> Unit,
) {
    val state by p.uiState.collectAsState()
    CollectionsScreen(
        uiState = state,
        onOpenCreateEditor = p::openCreateEditor,
        onOpenRenameEditor = p::openRenameEditor,
        onUpdateEditorName = p::updateEditorName,
        onConfirmEditor = p::confirmEditor,
        onDismissEditor = p::dismissEditor,
        onOpenDeleteConfirmation = p::openDeleteConfirmation,
        onConfirmDelete = p::confirmDelete,
        onDismissDeleteConfirmation = p::dismissDeleteConfirmation,
        onDismissError = p::dismissError,
        onOpenCollection = open,
        onBack = back,
    )
}

@Composable
private fun CollectionDetails(
    collection: pl.recipesforsoftware.signalbrief.domain.model.Collection,
    repository: CollectionsRepository,
    articleClick: (Article) -> Unit,
    back: () -> Unit,
) {
    val presenter = remember(collection.id) { CollectionDetailsPresenter(collection, repository) }
    DisposableEffect(presenter) { onDispose(presenter::dispose) }
    val state by presenter.uiState.collectAsState()
    CollectionDetailsScreen(state, articleClick, back)
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
    monitoring: () -> Unit,
    back: () -> Unit,
) {
    val p = remember { factory(initial) }
    DisposableEffect(p) { onDispose(p::dispose) }
    val query by p.query.collectAsState()
    val state by p.uiState.collectAsState()
    SearchScreen(query, {
        p.setQuery(it)
        changed(it)
    }, state, click, p::toggleBookmark, monitoring, back)
}

@Composable
private fun TopicMonitoring(
    p: TopicMonitoringPresenter,
    openMatches: (MonitoredTopic) -> Unit,
    back: () -> Unit,
) {
    val state by p.uiState.collectAsState()
    TopicMonitoringScreen(
        state,
        p::openCreateEditor,
        p::openRenameEditor,
        p::updateEditorQuery,
        p::confirmEditor,
        p::dismissEditor,
        p::openDeleteConfirmation,
        p::confirmDelete,
        p::dismissDeleteConfirmation,
        p::dismissError,
        openMatches,
        back,
    )
}

@Composable
private fun TopicMatches(
    factory: (MonitoredTopic) -> TopicMatchesPresenter,
    topic: MonitoredTopic,
    articleClick: (Article) -> Unit,
    back: () -> Unit,
) {
    val p = remember(topic.id) { factory(topic) }
    DisposableEffect(p) { onDispose(p::dispose) }
    val state by p.uiState.collectAsState()
    TopicMatchesScreen(state, articleClick, p::toggleBookmark, back)
}

@Composable private fun Details(
    article: Article,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    back: () -> Unit,
    manageCollections: () -> Unit,
) {
    val presenter = remember(article.url) { ArticleDetailsPresenter(savedArticlesRepository, article) }
    val assignmentPresenter =
        remember(article.url) { ArticleCollectionAssignmentPresenter(collectionsRepository, article) }
    DisposableEffect(presenter, assignmentPresenter) {
        onDispose {
            presenter.dispose()
            assignmentPresenter.dispose()
        }
    }
    val state by presenter.uiState.collectAsState()
    val assignmentState by assignmentPresenter.uiState.collectAsState()
    val uri = LocalUriHandler.current
    ArticleDetailsScreen(
        uiState = state,
        onBack = back,
        onBookmarkClick = presenter::toggleBookmark,
        onOpenFullArticle = { if (article.hasActionableUrl()) uri.openUri(article.url) },
        collectionAssignmentUiState = assignmentState,
        onCollectionAssignmentClick = assignmentPresenter::showPicker,
        onToggleCollection = assignmentPresenter::toggleCollection,
        onDismissCollectionAssignment = assignmentPresenter::dismissPicker,
        onManageCollections = {
            assignmentPresenter.dismissPicker()
            manageCollections()
        },
    )
}
