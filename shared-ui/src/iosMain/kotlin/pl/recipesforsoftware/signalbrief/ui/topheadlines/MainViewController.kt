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
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private const val ONBOARDING_KEY = "pl.recipesforsoftware.signalbrief.onboarding.completed"

/**
 * iOS composition root, invoked from SwiftUI as `MainViewControllerKt.mainViewController()`.
 *
 * Assembles the shared data layer and the shared app shell. Onboarding completion
 * is read synchronously from [NSUserDefaults] before Compose starts, so returning
 * users never see an onboarding flash. The shared HTTP client and the Room database
 * are created here and closed together when the view controller disappears.
 */
fun mainViewController(): UIViewController {
    val onboardingCompleted = readOnboardingCompleted()

    return ComposeUIViewController {
        SignalBriefTheme {
            var completed by remember { mutableStateOf(onboardingCompleted) }

            SignalBriefApp(
                onboardingCompleted = completed,
                onCompleteOnboarding = {
                    setOnboardingCompleted(true)
                    completed = true
                },
                topHeadlinesContent = { TopHeadlinesRoute() },
            )
        }
    }
}

@Composable
private fun TopHeadlinesRoute() {
    val composition = remember { createIosTopHeadlinesComposition() }
    val presenter = composition.presenter
    val uiState by presenter.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val openArticle = rememberOpenArticleAction(uriHandler)

    DisposableEffect(composition) {
        onDispose { composition.dispose() }
    }

    TopHeadlinesScreen(
        uiState = uiState,
        onRefresh = presenter::refresh,
        onArticleClick = openArticle,
        onBookmarkClick = presenter::toggleBookmark,
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
 * Holds the iOS composition root and its externally owned resources. The shared
 * HTTP client and the Room database are created here and closed here, so the view
 * controller teardown leaves no client or database instance behind.
 */
private class IosTopHeadlinesComposition(
    val presenter: TopHeadlinesPresenter,
    private val client: HttpClient,
    private val database: SignalBriefDatabase,
) {
    fun dispose() {
        presenter.dispose()
        client.close()
        database.close()
    }
}

private fun createIosTopHeadlinesComposition(): IosTopHeadlinesComposition {
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
    val presenter =
        TopHeadlinesPresenter(
            repository = OfflineFirstNewsRepository(remoteDataSource, localDataSource),
            savedArticlesRepository = savedArticlesRepository,
        )
    return IosTopHeadlinesComposition(presenter, client, database)
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
