package pl.recipesforsoftware.signalbrief.ui.main

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsViewModel
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefPresenter
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchPresenter
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DarkModeMenu
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var savedArticlesRepository: SavedArticlesRepository

    @Inject
    lateinit var newsRepository: NewsRepository

    @Inject
    lateinit var collectionsRepository: CollectionsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SignalBriefContent()
        }
    }

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
                topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
                    TopHeadlinesRoute(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = themeViewModel::toggleDarkMode,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onSearchClick = onSearchClick,
                    )
                },
                savedContent = { bottomBar, onArticleClick, onCollectionsClick ->
                    SavedArticlesRoute(
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onCollectionsClick = onCollectionsClick,
                    )
                },
                dailyBriefContent = { bottomBar, onArticleClick ->
                    DailyBriefRoute(bottomBar, onArticleClick)
                },
                searchContent = { initialQuery, onQueryChange, onArticleClick, onBack ->
                    SearchRoute(
                        initialQuery = initialQuery,
                        onQueryChange = onQueryChange,
                        onArticleClick = onArticleClick,
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
                collectionsContent = { onBack -> CollectionsRoute(onBack) },
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
    ) {
        val viewModel: TopHeadlinesViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        TopHeadlinesScreen(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            onArticleClick = onArticleClick,
            onBookmarkClick = viewModel::toggleBookmark,
            onSearchClick = onSearchClick,
            topBarActions = {
                DarkModeMenu(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                )
            },
            bottomBar = bottomBar,
        )
    }

    @Composable
    private fun SavedArticlesRoute(
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onCollectionsClick: () -> Unit,
    ) {
        val viewModel: SavedArticlesViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsState()

        SavedArticlesScreen(
            uiState = uiState,
            onArticleClick = onArticleClick,
            onRemoveClick = { viewModel.removeArticle(it.url) },
            onCollectionsClick = onCollectionsClick,
            bottomBar = bottomBar,
        )
    }

    @Composable
    private fun CollectionsRoute(onBack: () -> Unit) {
        BackHandler(onBack = onBack)
        val viewModel: CollectionsViewModel = hiltViewModel()
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
            onBack = onBack,
        )
    }

    @Composable
    private fun DailyBriefRoute(
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
    ) {
        val presenter =
            remember {
                DailyBriefPresenter(
                    newsRepository = newsRepository,
                    savedArticlesRepository = savedArticlesRepository,
                    dispatcher = Dispatchers.Main.immediate,
                )
            }
        DisposableEffect(presenter) { onDispose { presenter.dispose() } }
        val uiState by presenter.uiState.collectAsState()
        DailyBriefScreen(
            uiState = uiState,
            onArticleClick = onArticleClick,
            onBookmarkClick = presenter::toggleBookmark,
            bottomBar = bottomBar,
        )
    }

    /**
     * Android Local Search route.
     *
     * [BackHandler] integrates the Android system back gesture with the shared
     * child-navigation state: while Search is composed, system back closes Search
     * and returns to Headlines. The presenter is scoped to this route's
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
        onBack: () -> Unit,
    ) {
        BackHandler(onBack = onBack)

        val presenter =
            remember {
                SearchPresenter(
                    newsRepository = newsRepository,
                    savedArticlesRepository = savedArticlesRepository,
                    initialQuery = initialQuery,
                    dispatcher = Dispatchers.Main.immediate,
                )
            }
        DisposableEffect(presenter) {
            onDispose { presenter.dispose() }
        }

        val query by presenter.query.collectAsState()
        val uiState by presenter.uiState.collectAsState()

        SearchScreen(
            query = query,
            onQueryChange = {
                presenter.setQuery(it)
                onQueryChange(it)
            },
            uiState = uiState,
            onArticleClick = onArticleClick,
            onBookmarkClick = presenter::toggleBookmark,
            onBack = onBack,
        )
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
 * The presenter is scoped to this route's composition (created per article URL
 * and disposed on leave) so bookmark observation never outlives the screen;
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

    val presenter =
        remember(article.url) {
            ArticleDetailsPresenter(
                savedArticlesRepository = savedArticlesRepository,
                article = article,
                dispatcher = Dispatchers.Main.immediate,
            )
        }
    val assignmentPresenter =
        remember(article.url) {
            ArticleCollectionAssignmentPresenter(
                collectionsRepository = collectionsRepository,
                article = article,
                dispatcher = Dispatchers.Main.immediate,
            )
        }
    DisposableEffect(presenter, assignmentPresenter) {
        onDispose {
            presenter.dispose()
            assignmentPresenter.dispose()
        }
    }

    val uiState by presenter.uiState.collectAsState()
    val assignmentUiState by assignmentPresenter.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val openFullArticle =
        remember(article.url, uriHandler) {
            {
                if (article.hasActionableUrl()) {
                    uriHandler.openUri(article.url)
                }
            }
        }

    ArticleDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onBookmarkClick = presenter::toggleBookmark,
        onOpenFullArticle = openFullArticle,
        collectionAssignmentUiState = assignmentUiState,
        onCollectionAssignmentClick = assignmentPresenter::showPicker,
        onToggleCollection = assignmentPresenter::toggleCollection,
        onDismissCollectionAssignment = assignmentPresenter::dismissPicker,
        onManageCollections = {
            assignmentPresenter.dismissPicker()
            onManageCollections()
        },
    )
}
