package pl.recipesforsoftware.signalbrief.ui.main

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
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
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DarkModeMenu
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var savedArticlesRepository: SavedArticlesRepository

    @Inject
    lateinit var newsRepository: NewsRepository

    @Inject
    lateinit var collectionsRepository: CollectionsRepository

    @Inject
    lateinit var topicMonitoringRepository: TopicMonitoringRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SignalBriefContent()
        }
    }

    @Suppress("LongMethod")
    @Composable
    private fun SignalBriefContent() {
        val themeViewModel: ThemeViewModel = hiltViewModel()
        val isDarkMode by themeViewModel.isDarkMode.collectAsState()

        val onboardingViewModel: OnboardingViewModel = hiltViewModel()
        val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()
        val savedArticles by savedArticlesRepository.observeAllSavedArticles().collectAsState(emptyList())

        SyncSystemBars(isDarkMode)

        // Keep a local optimistic copy so the UI switches immediately after
        // the user completes onboarding while DataStore propagates the value.
        var localOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

        SignalBriefAndroidTheme(isDarkMode = isDarkMode) {
            val resolvedCompleted = localOnboardingCompleted ?: onboardingCompleted

            SignalBriefApp(
                onboardingCompleted = resolvedCompleted,
                onCompleteOnboarding = {
                    localOnboardingCompleted = true
                    onboardingViewModel.completeOnboarding()
                },
                topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                    TopHeadlinesRoute(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = themeViewModel::toggleDarkMode,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick,
                    )
                },
                savedContent = { bottomBar, onArticleClick, onCollectionsClick ->
                    SavedArticlesRoute(
                        savedArticlesRepository = savedArticlesRepository,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onCollectionsClick = onCollectionsClick,
                    )
                },
                dailyBriefContent = { bottomBar, onArticleClick ->
                    DailyBriefRoute(bottomBar, onArticleClick)
                },
                searchContent = { initialQuery, onQueryChange, onArticleClick, onOpenTopicMonitoring, onBack ->
                    SearchRoute(
                        initialQuery = initialQuery,
                        onQueryChange = onQueryChange,
                        onArticleClick = onArticleClick,
                        onOpenTopicMonitoring = onOpenTopicMonitoring,
                        onBack = onBack,
                    )
                },
                articleDetailsContent = { article, onBack, onCollectionsClick ->
                    ArticleDetailsRoute(
                        article = article,
                        savedArticlesRepository = savedArticlesRepository,
                        collectionsRepository = collectionsRepository,
                        onBack = onBack,
                        onManageCollections = onCollectionsClick,
                    )
                },
                collectionsContent = { onBack, onCollectionClick -> CollectionsRoute(onBack, onCollectionClick) },
                collectionDetailsContent = { collection, onArticleClick, onBack ->
                    CollectionDetailsRoute(collection, onArticleClick, onBack)
                },
                topicMonitoringContent = { openMatches, onBack -> TopicMonitoringRoute(openMatches, onBack) },
                topicMatchesContent = { topic, onArticleClick, onBack ->
                    TopicMatchesRoute(topic, onArticleClick, onBack)
                },
                settingsContent = { onBack ->
                    SettingsRoute(
                        newsRepository = newsRepository,
                        onBack = onBack,
                    )
                },
                savedArticleCount = savedArticles.size,
            )
        }
    }

    @Composable
    private fun SyncSystemBars(isDarkMode: Boolean) {
        // Keep system-bar icon appearance in sync with SignalBrief's own dark
        // mode preference instead of the Android system theme.
        SideEffect {
            val barStyle =
                if (isDarkMode) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }

            enableEdgeToEdge(
                statusBarStyle = barStyle,
                navigationBarStyle = barStyle,
            )
        }
    }

    @Composable
    private fun TopHeadlinesRoute(
        isDarkMode: Boolean,
        onToggleDarkMode: () -> Unit,
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onSearchClick: () -> Unit,
        onSettingsClick: () -> Unit,
    ) {
        ScreenViewModelScope {
            val viewModel: TopHeadlinesViewModel =
                viewModel { TopHeadlinesViewModel(newsRepository, savedArticlesRepository) }
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
    private fun SavedArticlesRoute(
        savedArticlesRepository: SavedArticlesRepository,
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onCollectionsClick: () -> Unit,
    ) {
        ScreenViewModelScope {
            val viewModel: SavedArticlesViewModel = viewModel { SavedArticlesViewModel(savedArticlesRepository) }
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
    private fun CollectionsRoute(
        onBack: () -> Unit,
        onCollectionClick: (pl.recipesforsoftware.signalbrief.domain.model.Collection) -> Unit,
    ) {
        BackHandler(onBack = onBack)
        ScreenViewModelScope {
            val viewModel: CollectionsViewModel = viewModel { CollectionsViewModel(collectionsRepository) }
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
    private fun CollectionDetailsRoute(
        collection: pl.recipesforsoftware.signalbrief.domain.model.Collection,
        onArticleClick: (Article) -> Unit,
        onBack: () -> Unit,
    ) {
        BackHandler(onBack = onBack)
        key(collection.id) {
            ScreenViewModelScope {
                val viewModel: CollectionDetailsViewModel =
                    viewModel { CollectionDetailsViewModel(collection, collectionsRepository) }
                val uiState by viewModel.uiState.collectAsState()
                CollectionDetailsScreen(uiState, onArticleClick, onBack)
            }
        }
    }

    @Composable
    private fun DailyBriefRoute(
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
    ) {
        ScreenViewModelScope {
            val viewModel: DailyBriefViewModel =
                viewModel { DailyBriefViewModel(newsRepository, savedArticlesRepository) }
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
     * [rememberSaveable] by [SignalBriefApp] so it survives configuration changes
     * and the Search -> Details -> Search round-trip.
     */
    @Composable
    private fun SearchRoute(
        initialQuery: String,
        onQueryChange: (String) -> Unit,
        onArticleClick: (Article) -> Unit,
        onOpenTopicMonitoring: () -> Unit,
        onBack: () -> Unit,
    ) {
        BackHandler(onBack = onBack)

        ScreenViewModelScope {
            val viewModel: SearchViewModel =
                viewModel { SearchViewModel(newsRepository, savedArticlesRepository, initialQuery) }
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
    private fun TopicMonitoringRoute(
        onOpenTopicMatches: (MonitoredTopic) -> Unit,
        onBack: () -> Unit,
    ) {
        BackHandler(onBack = onBack)
        ScreenViewModelScope {
            val viewModel: TopicMonitoringViewModel =
                viewModel { TopicMonitoringViewModel(topicMonitoringRepository, newsRepository) }
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
    private fun TopicMatchesRoute(
        topic: MonitoredTopic,
        onArticleClick: (Article) -> Unit,
        onBack: () -> Unit,
    ) {
        BackHandler(onBack = onBack)
        key(topic.id) {
            ScreenViewModelScope {
                val viewModel: TopicMatchesViewModel =
                    viewModel { TopicMatchesViewModel(topic, newsRepository, savedArticlesRepository) }
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
 * persistence itself stays in the existing Hilt singleton repository.
 */
@Composable
private fun ArticleDetailsRoute(
    article: Article,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    onBack: () -> Unit,
    onManageCollections: () -> Unit,
) {
    BackHandler(onBack = onBack)

    key(article.url) {
        ScreenViewModelScope {
            val viewModel: ArticleDetailsViewModel =
                viewModel { ArticleDetailsViewModel(savedArticlesRepository, article) }
            val assignmentViewModel: ArticleCollectionAssignmentViewModel =
                viewModel { ArticleCollectionAssignmentViewModel(collectionsRepository, article) }
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
private fun SettingsRoute(
    newsRepository: NewsRepository,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    ScreenViewModelScope {
        val viewModel: SettingsViewModel = viewModel { SettingsViewModel(newsRepository) }
        val uiState by viewModel.uiState.collectAsState()

        SettingsScreen(
            uiState = uiState,
            onBack = onBack,
            onClearDownloadedHeadlines = viewModel::clearDownloadedHeadlines,
        )
    }
}
