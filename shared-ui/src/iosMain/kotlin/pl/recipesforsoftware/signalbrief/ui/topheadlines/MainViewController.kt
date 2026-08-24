package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import pl.recipesforsoftware.signalbrief.data.local.RoomNewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createSignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.remote.KtorNewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import pl.recipesforsoftware.signalbrief.data.repository.OfflineFirstNewsRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomSavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsPresenter
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesPresenter
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchPresenter
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private const val ONBOARDING_KEY = "pl.recipesforsoftware.signalbrief.onboarding.completed"

/**
 * iOS composition root, invoked from SwiftUI as `MainViewControllerKt.mainViewController()`.
 *
 * Assembles the shared data layer and the shared app shell. Onboarding completion
 * is read synchronously from [NSUserDefaults] before Compose starts, so returning
 * users never see an onboarding flash.
 *
 * The iOS composition is created exactly once at the root of this controller and
 * disposed only when the whole controller is torn down. Headlines, Saved, Search,
 * and Article Details share the same [SignalBriefDatabase], the same
 * [RoomSavedArticlesRepository], and the same presenters, so switching tabs or
 * opening details never closes or recreates persistence layers.
 */
fun mainViewController(): UIViewController {
    val onboardingCompleted = readOnboardingCompleted()

    return ComposeUIViewController {
        val composition = remember { createIosComposition() }

        DisposableEffect(composition) {
            onDispose { composition.dispose() }
        }

        SignalBriefTheme {
            var completed by remember { mutableStateOf(onboardingCompleted) }

            SignalBriefApp(
                onboardingCompleted = completed,
                onCompleteOnboarding = {
                    setOnboardingCompleted(true)
                    completed = true
                },
                topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
                    HeadlinesRoute(
                        presenter = composition.headlinesPresenter,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                        onSearchClick = onSearchClick,
                    )
                },
                savedContent = { bottomBar, onArticleClick ->
                    SavedRoute(
                        presenter = composition.savedPresenter,
                        bottomBar = bottomBar,
                        onArticleClick = onArticleClick,
                    )
                },
                searchContent = { initialQuery, onQueryChange, onArticleClick, onBack ->
                    SearchRoute(
                        presenterFactory = composition::searchPresenter,
                        initialQuery = initialQuery,
                        onQueryChange = onQueryChange,
                        onArticleClick = onArticleClick,
                        onBack = onBack,
                    )
                },
                articleDetailsContent = { article, onBack ->
                    ArticleDetailsRoute(
                        article = article,
                        savedArticlesRepository = composition.savedArticlesRepository,
                        onBack = onBack,
                    )
                },
            )
        }
    }
}

@Composable
private fun HeadlinesRoute(
    presenter: TopHeadlinesPresenter,
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
    onSearchClick: () -> Unit,
) {
    val uiState by presenter.uiState.collectAsState()

    TopHeadlinesScreen(
        uiState = uiState,
        onRefresh = presenter::refresh,
        onArticleClick = onArticleClick,
        onBookmarkClick = presenter::toggleBookmark,
        onSearchClick = onSearchClick,
        bottomBar = bottomBar,
    )
}

@Composable
private fun SavedRoute(
    presenter: SavedArticlesPresenter,
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
) {
    val uiState by presenter.uiState.collectAsState()

    SavedArticlesScreen(
        uiState = uiState,
        onArticleClick = onArticleClick,
        onRemoveClick = { presenter.removeArticle(it.url) },
        bottomBar = bottomBar,
    )
}

@Composable
private fun SearchRoute(
    presenterFactory: (String) -> SearchPresenter,
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
) {
    val presenter = remember { presenterFactory(initialQuery) }
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

@Composable
private fun ArticleDetailsRoute(
    article: Article,
    savedArticlesRepository: SavedArticlesRepository,
    onBack: () -> Unit,
) {
    val presenter =
        remember(article.url) {
            ArticleDetailsPresenter(
                savedArticlesRepository = savedArticlesRepository,
                article = article,
            )
        }
    DisposableEffect(presenter) {
        onDispose { presenter.dispose() }
    }

    val uiState by presenter.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val openFullArticle = rememberOpenFullArticleAction(article, uriHandler)

    ArticleDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onBookmarkClick = presenter::toggleBookmark,
        onOpenFullArticle = openFullArticle,
    )
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
 * [RoomSavedArticlesRepository] instance are created once and live here. Both
 * presenters share the same repository instance so the Saved flow synchronizes
 * Headlines bookmark state through the same persistence layer.
 *
 * Article Details presenters are scoped to the details route composition and
 * are not held here; they receive the same repository instance without creating
 * a second database or repository.
 *
 * [dispose] must be called exactly once, when the owning composition root is
 * torn down; it cancels both presenters and then closes the client and database.
 */
private class IosComposition(
    val headlinesPresenter: TopHeadlinesPresenter,
    val savedPresenter: SavedArticlesPresenter,
    val savedArticlesRepository: SavedArticlesRepository,
    private val newsRepository: NewsRepository,
    private val client: HttpClient,
    private val database: SignalBriefDatabase,
) {
    fun searchPresenter(initialQuery: String): SearchPresenter =
        SearchPresenter(
            newsRepository = newsRepository,
            savedArticlesRepository = savedArticlesRepository,
            initialQuery = initialQuery,
        )

    fun dispose() {
        headlinesPresenter.dispose()
        savedPresenter.dispose()
        client.close()
        database.close()
    }
}

/**
 * Creates the single iOS composition graph.
 *
 * This factory is called exactly once by [mainViewController]. It constructs one
 * database, one repository, one HTTP client, and both presenters, then returns
 * an [IosComposition] that owns disposal of all those resources.
 */
private fun createIosComposition(): IosComposition {
    val apiKey =
        readNewsApiKeyFromBundle()
            ?: error(
                "NEWS_API_KEY is missing or empty. " +
                    "Configure iosApp/Secrets.xcconfig (copy Secrets.example.xcconfig) " +
                    "and rebuild the app.",
            )
    val config = NewsApiConfig(apiKey = apiKey, baseUrl = "https://newsapi.org/v2/")
    val client = createHttpClient(config)
    val database = createSignalBriefDatabase()
    val remoteDataSource = KtorNewsRemoteDataSource(client)
    val localDataSource = RoomNewsLocalDataSource(database)
    val savedArticlesRepository = RoomSavedArticlesRepository(database)
    val newsRepository = OfflineFirstNewsRepository(remoteDataSource, localDataSource)
    val headlinesPresenter =
        TopHeadlinesPresenter(
            repository = newsRepository,
            savedArticlesRepository = savedArticlesRepository,
        )
    val savedPresenter =
        SavedArticlesPresenter(
            savedArticlesRepository = savedArticlesRepository,
        )
    return IosComposition(
        headlinesPresenter,
        savedPresenter,
        savedArticlesRepository,
        newsRepository,
        client,
        database,
    )
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
