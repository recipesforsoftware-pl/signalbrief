package pl.recipesforsoftware.signalbrief.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
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
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DarkModeMenu
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel

@Composable
internal fun TopHeadlinesRoute(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: TopHeadlinesViewModel =
            koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        TopHeadlinesScreen(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            onArticleClick = onArticleClick,
            onBookmarkClick = viewModel::toggleBookmark,
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            topBarActions = {
                DarkModeMenu(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                )
            },
            bottomBar = bottomBar,
        )
    }
}

@Composable
internal fun SavedArticlesRoute(
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
    onCollectionsClick: () -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: SavedArticlesViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()

        SavedArticlesScreen(
            uiState = uiState,
            onArticleClick = onArticleClick,
            onRemoveClick = { viewModel.removeArticle(it.url) },
            onCollectionsClick = onCollectionsClick,
            bottomBar = bottomBar,
        )
    }
}

@Composable
internal fun CollectionsRoute(
    onBack: () -> Unit,
    onCollectionClick: (pl.recipesforsoftware.signalbrief.domain.model.Collection) -> Unit,
) {
    BackHandler(onBack = onBack)
    ScreenViewModelScope {
        val viewModel: CollectionsViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        CollectionsScreen(
            uiState = uiState,
            onOpenCreateEditor = viewModel::openCreateEditor,
            onOpenRenameEditor = viewModel::openRenameEditor,
            onUpdateEditorName = viewModel::updateEditorName,
            onConfirmEditor = viewModel::confirmEditor,
            onDismissEditor = viewModel::dismissEditor,
            onOpenDeleteConfirmation = viewModel::openDeleteConfirmation,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
            onDismissError = viewModel::dismissError,
            onOpenCollection = onCollectionClick,
            onBack = onBack,
        )
    }
}

@Composable
internal fun CollectionDetailsRoute(
    collection: pl.recipesforsoftware.signalbrief.domain.model.Collection,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    key(collection.id) {
        ScreenViewModelScope {
            val viewModel: CollectionDetailsViewModel =
                koinViewModel(parameters = { parametersOf(collection) })
            val uiState by viewModel.uiState.collectAsState()
            CollectionDetailsScreen(uiState, onArticleClick, onBack)
        }
    }
}

@Composable
internal fun DailyBriefRoute(
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: DailyBriefViewModel =
            koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        DailyBriefScreen(
            uiState = uiState,
            onArticleClick = onArticleClick,
            onBookmarkClick = viewModel::toggleBookmark,
            bottomBar = bottomBar,
        )
    }
}

/**
 * Android Local Search route.
 *
 * [BackHandler] integrates the Android system back gesture with the shared
 * child-navigation state: while Search is composed, system back closes Search
 * and returns to Headlines. The ViewModel is scoped to this route's
 * composition (created once and disposed on leave) so query state lives only
 * as long as the screen is visible. The query itself is hoisted into
 * `rememberSaveable` by `SignalBriefApp` so it survives configuration changes
 * and the Search -> Details -> Search round-trip.
 */
@Composable
internal fun SearchRoute(
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onArticleClick: (Article) -> Unit,
    onOpenTopicMonitoring: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    ScreenViewModelScope {
        val viewModel: SearchViewModel =
            koinViewModel(parameters = { parametersOf(initialQuery) })
        val query by viewModel.query.collectAsState()
        val uiState by viewModel.uiState.collectAsState()
        SearchScreen(
            query = query,
            onQueryChange = {
                viewModel.setQuery(it)
                onQueryChange(it)
            },
            uiState = uiState,
            onArticleClick = onArticleClick,
            onBookmarkClick = viewModel::toggleBookmark,
            onOpenTopicMonitoring = onOpenTopicMonitoring,
            onBack = onBack,
        )
    }
}

@Composable
internal fun TopicMonitoringRoute(
    onOpenTopicMatches: (MonitoredTopic) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    ScreenViewModelScope {
        val viewModel: TopicMonitoringViewModel =
            koinViewModel()
        val uiState by viewModel.uiState.collectAsState()
        TopicMonitoringScreen(
            uiState,
            viewModel::openCreateEditor,
            viewModel::openRenameEditor,
            viewModel::updateEditorQuery,
            viewModel::confirmEditor,
            viewModel::dismissEditor,
            viewModel::openDeleteConfirmation,
            viewModel::confirmDelete,
            viewModel::dismissDeleteConfirmation,
            viewModel::dismissError,
            onOpenTopicMatches,
            onBack,
        )
    }
}

@Composable
internal fun TopicMatchesRoute(
    topic: MonitoredTopic,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    key(topic.id) {
        ScreenViewModelScope {
            val viewModel: TopicMatchesViewModel =
                koinViewModel(parameters = { parametersOf(topic) })
            val uiState by viewModel.uiState.collectAsState()
            TopicMatchesScreen(
                uiState = uiState,
                onArticleClick = onArticleClick,
                onBookmarkClick = viewModel::toggleBookmark,
                onBack = onBack,
            )
        }
    }
}

/**
 * Android Article Details route.
 *
 * [BackHandler] integrates the Android system back gesture with the shared
 * child-navigation state: while details are composed, system back clears the
 * selected article and returns to the originating destination — exactly what
 * the toolbar back action does. When details leave composition the handler
 * disables itself, so back behaves normally everywhere else. No navigation
 * library and no back stack are involved.
 *
 * The ViewModels are scoped to this route's composition (created per article URL)
 * so bookmark observation never outlives the screen;
 * persistence itself stays in the existing singleton repository.
 */
@Composable
internal fun ArticleDetailsRoute(
    article: Article,
    onBack: () -> Unit,
    onManageCollections: () -> Unit,
) {
    BackHandler(onBack = onBack)

    key(article.url) {
        ScreenViewModelScope {
            val viewModel: ArticleDetailsViewModel =
                koinViewModel(parameters = { parametersOf(article) })
            val assignmentViewModel: ArticleCollectionAssignmentViewModel =
                koinViewModel(parameters = { parametersOf(article) })
            val uiState by viewModel.uiState.collectAsState()
            val assignmentUiState by assignmentViewModel.uiState.collectAsState()
            val uriHandler = LocalUriHandler.current
            val openFullArticle =
                remember(article.url, uriHandler) {
                    { if (article.hasActionableUrl()) uriHandler.openUri(article.url) }
                }
            ArticleDetailsScreen(
                uiState = uiState,
                onBack = onBack,
                onBookmarkClick = viewModel::toggleBookmark,
                onOpenFullArticle = openFullArticle,
                collectionAssignmentUiState = assignmentUiState,
                onCollectionAssignmentClick = assignmentViewModel::showPicker,
                onToggleCollection = assignmentViewModel::toggleCollection,
                onDismissCollectionAssignment = assignmentViewModel::dismissPicker,
                onManageCollections = {
                    assignmentViewModel.dismissPicker()
                    onManageCollections()
                },
            )
        }
    }
}

/**
 * Android Settings route.
 *
 * [BackHandler] integrates the Android system back gesture with the shared
 * child-navigation state: while Settings is composed, system back closes
 * Settings and returns to Headlines. [ScreenViewModelScope] owns the Settings
 * ViewModel only while this route is composed, clearing it when the route
 * leaves composition. It observes only locally cached headlines; no network
 * work is triggered.
 */
@Composable
internal fun SettingsRoute(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    ScreenViewModelScope {
        val viewModel: SettingsViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsState()

        SettingsScreen(
            uiState = uiState,
            onBack = onBack,
            onClearDownloadedHeadlines = viewModel::clearDownloadedHeadlines,
        )
    }
}
