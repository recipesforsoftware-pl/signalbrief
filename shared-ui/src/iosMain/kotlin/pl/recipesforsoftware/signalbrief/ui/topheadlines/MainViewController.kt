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
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesPresenter
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
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
 * disposed only when the whole controller is torn down. Headlines and Saved share
 * the same [SignalBriefDatabase], the same [RoomSavedArticlesRepository], and the
 * same presenters, so switching tabs never closes or recreates persistence layers.
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
                topHeadlinesContent = { bottomBar ->
                    HeadlinesRoute(
                        presenter = composition.headlinesPresenter,
                        bottomBar = bottomBar,
                    )
                },
                savedContent = { bottomBar ->
                    SavedRoute(
                        presenter = composition.savedPresenter,
                        bottomBar = bottomBar,
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
) {
    val uiState by presenter.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val openArticle = rememberOpenArticleAction(uriHandler)

    TopHeadlinesScreen(
        uiState = uiState,
        onRefresh = presenter::refresh,
        onArticleClick = openArticle,
        onBookmarkClick = presenter::toggleBookmark,
        bottomBar = bottomBar,
    )
}

@Composable
private fun SavedRoute(
    presenter: SavedArticlesPresenter,
    bottomBar: @Composable () -> Unit,
) {
    val uiState by presenter.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val openArticle = rememberOpenArticleAction(uriHandler)

    SavedArticlesScreen(
        uiState = uiState,
        onArticleClick = openArticle,
        onRemoveClick = { presenter.removeArticle(it.url) },
        bottomBar = bottomBar,
    )
}

@Composable
private fun rememberOpenArticleAction(uriHandler: UriHandler): (Article) -> Unit =
    remember(uriHandler) {
        { article ->
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
 * [dispose] must be called exactly once, when the owning composition root is
 * torn down; it cancels both presenters and then closes the client and database.
 */
private class IosComposition(
    val headlinesPresenter: TopHeadlinesPresenter,
    val savedPresenter: SavedArticlesPresenter,
    private val client: HttpClient,
    private val database: SignalBriefDatabase,
) {
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
    val headlinesPresenter =
        TopHeadlinesPresenter(
            repository = OfflineFirstNewsRepository(remoteDataSource, localDataSource),
            savedArticlesRepository = savedArticlesRepository,
        )
    val savedPresenter =
        SavedArticlesPresenter(
            savedArticlesRepository = savedArticlesRepository,
        )
    return IosComposition(headlinesPresenter, savedPresenter, client, database)
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
