@file:Suppress("TooManyFunctions")

package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.error.InstanceCreationException
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import pl.recipesforsoftware.signalbrief.data.local.NewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.RoomNewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createSignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.remote.KtorNewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.data.remote.NewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import pl.recipesforsoftware.signalbrief.data.repository.OfflineFirstNewsRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomCollectionsRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomSavedArticlesRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomTopicMonitoringRepository
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
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsRoute
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefViewModel
import pl.recipesforsoftware.signalbrief.ui.lifecycle.ScreenViewModelScope
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private const val ONBOARDING_KEY = "pl.recipesforsoftware.signalbrief.onboarding.completed"

/**
 * iOS composition root, invoked from SwiftUI as `IosComposeHostKt.createIosComposeHost()`.
 *
 * Assembles the shared data layer and the shared app shell. Onboarding completion
 * is read synchronously from [NSUserDefaults] before Compose starts, so returning
 * users never see an onboarding flash.
 *
 * The iOS composition is created exactly once at the root of this Compose host and
 * disposed only when the whole Compose host is torn down. Headlines, Saved, Search,
 * and Article Details share the same [SignalBriefDatabase], the same
 * [RoomSavedArticlesRepository], and root-scoped ViewModels, so switching tabs
 * or opening details never closes or recreates persistence layers.
 */
@Suppress("LongMethod")
fun createIosComposeHost(): UIViewController {
    val onboardingCompleted = readOnboardingCompleted()

    return ComposeUIViewController {
        val composition = remember { createIosComposition() }

        DisposableEffect(composition) {
            onDispose { composition.dispose() }
        }

        SignalBriefTheme {
            var completed by remember { mutableStateOf(onboardingCompleted) }
            val savedArticles by
                composition.savedArticlesRepository.observeAllSavedArticles().collectAsState(emptyList())

            SignalBriefApp(
                onboardingCompleted = completed,
                onCompleteOnboarding = {
                    setOnboardingCompleted(true)
                    completed = true
                },
                topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                    HeadlinesRoute(
                        newsRepository = composition.newsRepository,
                        savedArticlesRepository = composition.savedArticlesRepository,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick,
                    )
                },
                savedContent = { bottomBar, onArticleClick, onCollectionsClick ->
                    SavedRoute(
                        savedArticlesRepository = composition.savedArticlesRepository,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onCollectionsClick = onCollectionsClick,
                    )
                },
                dailyBriefContent = { bottomBar, onArticleClick ->
                    DailyBriefRoute(
                        newsRepository = composition.newsRepository,
                        savedArticlesRepository = composition.savedArticlesRepository,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                    )
                },
                searchContent = { initialQuery, onQueryChange, onArticleClick, onOpenTopicMonitoring, onBack ->
                    SearchRoute(
                        newsRepository = composition.newsRepository,
                        savedArticlesRepository = composition.savedArticlesRepository,
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
                        savedArticlesRepository = composition.savedArticlesRepository,
                        collectionsRepository = composition.collectionsRepository,
                        onBack = onBack,
                        onManageCollections = onCollectionsClick,
                    )
                },
                collectionsContent = { onBack, onCollectionClick ->
                    CollectionsRoute(composition.collectionsRepository, onBack, onCollectionClick)
                },
                collectionDetailsContent = { collection, onArticleClick, onBack ->
                    CollectionDetailsRoute(collection, composition.collectionsRepository, onArticleClick, onBack)
                },
                topicMonitoringContent = { onOpenTopicMatches, onBack ->
                    TopicMonitoringRoute(
                        composition.topicMonitoringRepository,
                        composition.newsRepository,
                        onOpenTopicMatches,
                        onBack,
                    )
                },
                topicMatchesContent = { topic, onArticleClick, onBack ->
                    TopicMatchesRoute(
                        newsRepository = composition.newsRepository,
                        savedArticlesRepository = composition.savedArticlesRepository,
                        topic = topic,
                        onArticleClick = onArticleClick,
                        onBack = onBack,
                    )
                },
                settingsContent = { onBack ->
                    SettingsRoute(
                        newsRepository = composition.newsRepository,
                        onBack = onBack,
                    )
                },
                savedArticleCount = savedArticles.size,
            )
        }
    }
}

@Composable
private fun CollectionDetailsRoute(
    collection: pl.recipesforsoftware.signalbrief.domain.model.Collection,
    repository: CollectionsRepository,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
) {
    key(collection.id) {
        ScreenViewModelScope {
            val viewModel: CollectionDetailsViewModel = viewModel { CollectionDetailsViewModel(collection, repository) }
            val uiState by viewModel.uiState.collectAsState()
            CollectionDetailsScreen(uiState, onArticleClick, onBack)
        }
    }
}

@Composable
private fun HeadlinesRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
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
            bottomBar = bottomBar,
        )
    }
}

@Composable
private fun SavedRoute(
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
private fun DailyBriefRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: DailyBriefViewModel = viewModel { DailyBriefViewModel(newsRepository, savedArticlesRepository) }
        val uiState by viewModel.uiState.collectAsState()
        DailyBriefScreen(
            uiState = uiState,
            onArticleClick = onArticleClick,
            onBookmarkClick = viewModel::toggleBookmark,
            bottomBar = bottomBar,
        )
    }
}

@Composable
private fun SearchRoute(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onArticleClick: (Article) -> Unit,
    onOpenTopicMonitoring: () -> Unit,
    onBack: () -> Unit,
) {
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
    topicMonitoringRepository: TopicMonitoringRepository,
    newsRepository: NewsRepository,
    onOpenTopicMatches: (MonitoredTopic) -> Unit,
    onBack: () -> Unit,
) {
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
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    topic: MonitoredTopic,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
) {
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

@Composable
private fun SettingsRoute(
    newsRepository: NewsRepository,
    onBack: () -> Unit,
) {
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

@Composable
private fun ArticleDetailsRoute(
    article: Article,
    savedArticlesRepository: SavedArticlesRepository,
    collectionsRepository: CollectionsRepository,
    onBack: () -> Unit,
    onManageCollections: () -> Unit,
) {
    key(article.url) {
        ScreenViewModelScope {
            val viewModel: ArticleDetailsViewModel =
                viewModel { ArticleDetailsViewModel(savedArticlesRepository, article) }
            val assignmentViewModel: ArticleCollectionAssignmentViewModel =
                viewModel { ArticleCollectionAssignmentViewModel(collectionsRepository, article) }
            val uiState by viewModel.uiState.collectAsState()
            val assignmentUiState by assignmentViewModel.uiState.collectAsState()
            val uriHandler = LocalUriHandler.current
            val openFullArticle = rememberOpenFullArticleAction(article, uriHandler)
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

@Composable
private fun rememberOpenFullArticleAction(
    article: Article,
    uriHandler: UriHandler,
): () -> Unit =
    remember(article.url, uriHandler) {
        {
            if (article.hasActionableUrl()) {
                uriHandler.openUri(article.url)
            }
        }
    }

/**
 * Holds the single iOS composition root and its externally owned resources.
 *
 * The shared HTTP client, the Room database, and the single
 * [RoomSavedArticlesRepository] instance are created once and live here. Saved
 * and Headlines use the same repository instance so bookmark state synchronizes
 * through the same persistence layer.
 *
 * Article Details ViewModels are scoped to the details route composition and
 * are not held here; they receive the same repository instance without creating
 * a second database or repository.
 *
 * [dispose] is idempotent and must be called when the owning composition root
 * is torn down.
 */
internal class IosComposition(
    val savedArticlesRepository: SavedArticlesRepository,
    val collectionsRepository: CollectionsRepository,
    val topicMonitoringRepository: TopicMonitoringRepository,
    val newsRepository: NewsRepository,
    private val koinApplication: KoinApplication,
    private val resources: IosCompositionResources,
) {
    private var isDisposed = false

    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        try {
            resources.dispose()
        } finally {
            koinApplication.close()
        }
    }
}

/**
 * Creates the single iOS composition graph.
 *
 * The returned composition owns the platform resources while Koin owns only
 * object graph construction. Factory arguments keep construction isolated from
 * the app bundle and user storage in iOS tests.
 */
@Suppress("TooGenericExceptionCaught")
internal fun createIosComposition(
    apiKey: String,
    httpClientFactory: (NewsApiConfig) -> HttpClient = ::createHttpClient,
    databaseFactory: () -> SignalBriefDatabase = ::createSignalBriefDatabase,
): IosComposition {
    val config = NewsApiConfig(apiKey = apiKey, baseUrl = "https://newsapi.org/v2/")
    val resources = IosCompositionResources()
    val application =
        koinApplication {
            modules(
                iosModule(
                    config = config,
                    httpClientFactory = httpClientFactory,
                    databaseFactory = databaseFactory,
                    resources = resources,
                ),
            )
        }

    try {
        val koin = application.koin
        return IosComposition(
            newsRepository = koin.get(),
            savedArticlesRepository = koin.get(),
            collectionsRepository = koin.get(),
            topicMonitoringRepository = koin.get(),
            koinApplication = application,
            resources = resources,
        )
    } catch (exception: Exception) {
        val constructionFailure = exception.unwrapKoinCreationExceptions()
        runCatching(application::close)
            .exceptionOrNull()
            ?.let(constructionFailure::addSuppressed)
        resources.disposeAfterFailedConstruction(constructionFailure)
    }
}

private fun iosModule(
    config: NewsApiConfig,
    httpClientFactory: (NewsApiConfig) -> HttpClient,
    databaseFactory: () -> SignalBriefDatabase,
    resources: IosCompositionResources,
): Module =
    module {
        single { config }
        single<HttpClient> {
            httpClientFactory(get()).also(resources::registerHttpClient)
        }
        single<SignalBriefDatabase> {
            databaseFactory().also(resources::registerDatabase)
        }
        single<NewsRemoteDataSource> { KtorNewsRemoteDataSource(get()) }
        single<NewsLocalDataSource> { RoomNewsLocalDataSource(get()) }
        single<NewsRepository> { OfflineFirstNewsRepository(get(), get()) }
        single<SavedArticlesRepository> { RoomSavedArticlesRepository(get()) }
        single<CollectionsRepository> { RoomCollectionsRepository(get()) }
        single<TopicMonitoringRepository> { RoomTopicMonitoringRepository(get()) }
    }

/** Owns resources created while resolving the iOS Koin graph. */
internal class IosCompositionResources(
    private var closeHttpClient: (() -> Unit)? = null,
    private var closeDatabase: (() -> Unit)? = null,
) {
    private var isDisposed = false

    fun registerHttpClient(client: HttpClient) {
        check(closeHttpClient == null) { "HTTP client is already registered." }
        closeHttpClient = client::close
    }

    fun registerDatabase(database: SignalBriefDatabase) {
        check(closeDatabase == null) { "Database is already registered." }
        closeDatabase = database::close
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        try {
            closeHttpClient?.invoke()
        } finally {
            closeDatabase?.invoke()
        }
    }

    fun disposeAfterFailedConstruction(constructionFailure: Exception): Nothing {
        if (!isDisposed) {
            isDisposed = true
            runCatching { closeDatabase?.invoke() }
                .exceptionOrNull()
                ?.let(constructionFailure::addSuppressed)
            runCatching { closeHttpClient?.invoke() }
                .exceptionOrNull()
                ?.let(constructionFailure::addSuppressed)
        }
        throw constructionFailure
    }
}

private fun Exception.unwrapKoinCreationExceptions(): Exception {
    var current = this
    while (current is InstanceCreationException && current.cause is Exception) {
        current = current.cause as Exception
    }
    return current
}

private fun createIosComposition(): IosComposition {
    val apiKey =
        readNewsApiKeyFromBundle()
            ?: error(
                "NEWS_API_KEY is missing or empty. " +
                    "Configure iosApp/Secrets.xcconfig (copy Secrets.example.xcconfig) " +
                    "and rebuild the app.",
            )
    return createIosComposition(apiKey)
}

/**
 * Reads the NewsAPI key injected into Info.plist by the build via
 * `NEWS_API_KEY = $(NEWS_API_KEY)` from the git-ignored `Secrets.xcconfig`.
 */
private fun readNewsApiKeyFromBundle(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("NEWS_API_KEY") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun readOnboardingCompleted(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey(ONBOARDING_KEY)

private fun setOnboardingCompleted(completed: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(completed, ONBOARDING_KEY)
}
