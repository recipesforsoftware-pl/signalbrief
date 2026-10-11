@file:Suppress("TooManyFunctions")

package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentViewModel
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsViewModel
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefViewModel
import pl.recipesforsoftware.signalbrief.ui.lifecycle.ScreenViewModelScope
import pl.recipesforsoftware.signalbrief.ui.navigation.SignalBriefRoute
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel

/** Reusable contract-only host for browser and other lightweight platform compositions. */
@Composable
fun SignalBriefAppHost(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    topicMonitoringRepository: TopicMonitoringRepository,
) {
    val savedArticles by savedArticlesRepository.observeAllSavedArticles().collectAsState(emptyList())
    SignalBriefApp(onboardingCompleted = true, onCompleteOnboarding = {}) {
        SignalBriefNavHost(
            savedArticleCount = savedArticles.size,
            entries = {
                defaultSignalBriefEntries(
                    newsRepository,
                    savedArticlesRepository,
                    collectionsRepository,
                    topicMonitoringRepository,
                    it,
                )
            },
        )
    }
}

@Suppress("LongMethod")
internal fun EntryProviderScope<NavKey>.defaultSignalBriefEntries(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    topicMonitoringRepository: TopicMonitoringRepository,
    navigation: SignalBriefNavigationContext,
) {
    entry<SignalBriefRoute.Headlines> {
        TopHeadlinesRoute(
            newsRepository,
            savedArticlesRepository,
            navigation.bottomBarFor(SignalBriefRoute.Headlines),
            navigation.openArticle,
            navigation.openSearch,
            navigation.openSettings,
        )
    }
    entry<SignalBriefRoute.DailyBrief> {
        DailyBriefRoute(
            newsRepository,
            savedArticlesRepository,
            navigation.bottomBarFor(SignalBriefRoute.DailyBrief),
            navigation.openArticle,
        )
    }
    entry<SignalBriefRoute.Saved> {
        SavedArticlesRoute(
            savedArticlesRepository,
            navigation.bottomBarFor(SignalBriefRoute.Saved),
            navigation.openArticle,
            navigation.openCollections,
        )
    }
    entry<SignalBriefRoute.Search> {
        SearchRoute(
            newsRepository,
            savedArticlesRepository,
            navigation.searchQuery(),
            navigation.onSearchQueryChange,
            navigation.openArticle,
            navigation.openTopicMonitoring,
            navigation.back,
        )
    }
    entry<SignalBriefRoute.Settings> { SettingsRoute(newsRepository, navigation.back) }
    entry<SignalBriefRoute.TopicMonitoring> {
        TopicMonitoringRoute(topicMonitoringRepository, newsRepository, navigation.openTopicMatches, navigation.back)
    }
    entry<SignalBriefRoute.TopicMatches> { route ->
        navigation.topicFor(route.topicId)?.let {
            TopicMatchesRoute(newsRepository, savedArticlesRepository, it, navigation.openArticle, navigation.back)
        }
            ?: InvalidDestination(route.topicId, navigation.back)
    }
    entry<SignalBriefRoute.ArticleDetails> { route ->
        navigation.articleFor(route.articleUrl)?.let {
            ArticleDetailsRoute(
                it,
                savedArticlesRepository,
                collectionsRepository,
                navigation.back,
                navigation.openCollections,
            )
        }
            ?: InvalidDestination(route.articleUrl, navigation.back)
    }
    entry<SignalBriefRoute.Collections> {
        CollectionsRoute(
            collectionsRepository,
            navigation.back,
            navigation.openCollection,
        )
    }
    entry<SignalBriefRoute.CollectionDetails> { route ->
        navigation.collectionFor(route.collectionId)?.let {
            CollectionDetailsRoute(it, collectionsRepository, navigation.openArticle, navigation.back)
        }
            ?: InvalidDestination(route.collectionId, navigation.back)
    }
}

@Composable
internal fun TopHeadlinesRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
    search: () -> Unit,
    settings: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: TopHeadlinesViewModel =
            viewModel { TopHeadlinesViewModel(newsRepository, savedArticlesRepository) }
        val state by viewModel.uiState.collectAsState()
        TopHeadlinesScreen(
            state,
            viewModel::refresh,
            click,
            onBookmarkClick = viewModel::toggleBookmark,
            onSearchClick = search,
            onSettingsClick = settings,
            bottomBar = bottom,
        )
    }
}

@Composable internal fun SavedArticlesRoute(
    savedArticlesRepository: SavedArticlesRepository,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
    collections: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: SavedArticlesViewModel = viewModel { SavedArticlesViewModel(savedArticlesRepository) }
        val state by viewModel.uiState.collectAsState()
        SavedArticlesScreen(state, click, { viewModel.removeArticle(it.url) }, collections, bottomBar = bottom)
    }
}

@Composable
internal fun CollectionsRoute(
    collectionsRepository: CollectionsRepository,
    back: () -> Unit,
    open: (pl.recipesforsoftware.signalbrief.domain.model.Collection) -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: CollectionsViewModel = viewModel { CollectionsViewModel(collectionsRepository) }
        val state by viewModel.uiState.collectAsState()
        CollectionsScreen(
            uiState = state,
            onOpenCreateEditor = viewModel::openCreateEditor,
            onOpenRenameEditor = viewModel::openRenameEditor,
            onUpdateEditorName = viewModel::updateEditorName,
            onConfirmEditor = viewModel::confirmEditor,
            onDismissEditor = viewModel::dismissEditor,
            onOpenDeleteConfirmation = viewModel::openDeleteConfirmation,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
            onDismissError = viewModel::dismissError,
            onOpenCollection = open,
            onBack = back,
        )
    }
}

@Composable
internal fun CollectionDetailsRoute(
    collection: pl.recipesforsoftware.signalbrief.domain.model.Collection,
    repository: CollectionsRepository,
    articleClick: (Article) -> Unit,
    back: () -> Unit,
) {
    key(collection.id) {
        ScreenViewModelScope {
            val viewModel: CollectionDetailsViewModel = viewModel { CollectionDetailsViewModel(collection, repository) }
            val state by viewModel.uiState.collectAsState()
            CollectionDetailsScreen(state, articleClick, back)
        }
    }
}

@Composable internal fun DailyBriefRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    bottom: @Composable () -> Unit,
    click: (Article) -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: DailyBriefViewModel = viewModel { DailyBriefViewModel(newsRepository, savedArticlesRepository) }
        val state by viewModel.uiState.collectAsState()
        DailyBriefScreen(state, click, viewModel::toggleBookmark, bottomBar = bottom)
    }
}

@Composable
internal fun SearchRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    initial: String,
    changed: (String) -> Unit,
    click: (Article) -> Unit,
    monitoring: () -> Unit,
    back: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: SearchViewModel = viewModel { SearchViewModel(newsRepository, savedArticlesRepository, initial) }
        val query by viewModel.query.collectAsState()
        val state by viewModel.uiState.collectAsState()
        SearchScreen(query, {
            viewModel.setQuery(it)
            changed(it)
        }, state, click, viewModel::toggleBookmark, monitoring, back)
    }
}

@Composable
internal fun TopicMonitoringRoute(
    topicMonitoringRepository: TopicMonitoringRepository,
    newsRepository: NewsRepository,
    openMatches: (MonitoredTopic) -> Unit,
    back: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: TopicMonitoringViewModel =
            viewModel { TopicMonitoringViewModel(topicMonitoringRepository, newsRepository) }
        val state by viewModel.uiState.collectAsState()
        TopicMonitoringScreen(
            state,
            viewModel::openCreateEditor,
            viewModel::openRenameEditor,
            viewModel::updateEditorQuery,
            viewModel::confirmEditor,
            viewModel::dismissEditor,
            viewModel::openDeleteConfirmation,
            viewModel::confirmDelete,
            viewModel::dismissDeleteConfirmation,
            viewModel::dismissError,
            openMatches,
            back,
        )
    }
}

@Composable
internal fun TopicMatchesRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    topic: MonitoredTopic,
    articleClick: (Article) -> Unit,
    back: () -> Unit,
) {
    key(topic.id) {
        ScreenViewModelScope {
            val viewModel: TopicMatchesViewModel =
                viewModel { TopicMatchesViewModel(topic, newsRepository, savedArticlesRepository) }
            val state by viewModel.uiState.collectAsState()
            TopicMatchesScreen(state, articleClick, viewModel::toggleBookmark, back)
        }
    }
}

@Composable
internal fun SettingsRoute(
    newsRepository: NewsRepository,
    back: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: SettingsViewModel = viewModel { SettingsViewModel(newsRepository) }
        val state by viewModel.uiState.collectAsState()
        SettingsScreen(state, back, viewModel::clearDownloadedHeadlines)
    }
}

@Composable internal fun ArticleDetailsRoute(
    article: Article,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    back: () -> Unit,
    manageCollections: () -> Unit,
) {
    key(article.url) {
        ScreenViewModelScope {
            val viewModel: ArticleDetailsViewModel =
                viewModel { ArticleDetailsViewModel(savedArticlesRepository, article) }
            val assignmentViewModel: ArticleCollectionAssignmentViewModel =
                viewModel { ArticleCollectionAssignmentViewModel(collectionsRepository, article) }
            val state by viewModel.uiState.collectAsState()
            val assignmentState by assignmentViewModel.uiState.collectAsState()
            val uri = LocalUriHandler.current
            ArticleDetailsScreen(
                uiState = state,
                onBack = back,
                onBookmarkClick = viewModel::toggleBookmark,
                onOpenFullArticle = { if (article.hasActionableUrl()) uri.openUri(article.url) },
                collectionAssignmentUiState = assignmentState,
                onCollectionAssignmentClick = assignmentViewModel::showPicker,
                onToggleCollection = assignmentViewModel::toggleCollection,
                onDismissCollectionAssignment = assignmentViewModel::dismissPicker,
                onManageCollections = {
                    assignmentViewModel.dismissPicker()
                    manageCollections()
                },
            )
        }
    }
}
