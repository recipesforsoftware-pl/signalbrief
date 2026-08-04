package com.recipesforsoftware.mvvm.ui.topheadlines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.recipesforsoftware.mvvm.data.local.RoomNewsLocalDataSource
import com.recipesforsoftware.mvvm.data.local.db.SignalBriefDatabase
import com.recipesforsoftware.mvvm.data.local.db.createSignalBriefDatabase
import com.recipesforsoftware.mvvm.data.remote.KtorNewsRemoteDataSource
import com.recipesforsoftware.mvvm.data.remote.NewsApiConfig
import com.recipesforsoftware.mvvm.data.remote.createHttpClient
import com.recipesforsoftware.mvvm.data.repository.OfflineFirstNewsRepository
import io.ktor.client.HttpClient
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * iOS composition root, invoked from SwiftUI as `MainViewControllerKt.mainViewController()`.
 *
 * Assembles the shared data layer and [TopHeadlinesPresenter] without any
 * platform dependency injection framework, mirroring the Android wiring:
 * `NewsApiConfig` + [createHttpClient] + [KtorNewsRemoteDataSource] on the
 * remote side, `SignalBriefDatabase` + [RoomNewsLocalDataSource] on the local
 * side, combined into an [OfflineFirstNewsRepository] that implements the
 * `NewsRepository` contract consumed by the shared screen.
 */
fun mainViewController(): UIViewController =
    ComposeUIViewController {
        SignalBriefTheme {
            TopHeadlinesRoute()
        }
    }

@Composable
private fun TopHeadlinesRoute() {
    val composition = remember { createIosTopHeadlinesPresenter() }
    val presenter = composition.presenter
    val uiState by presenter.uiState.collectAsState()

    DisposableEffect(composition) {
        onDispose { composition.dispose() }
    }

    TopHeadlinesScreen(
        uiState = uiState,
        onRefresh = presenter::refresh,
    )
}

/**
 * Holds the iOS composition root and its externally owned resources. The shared
 * HTTP client and the Room database are created here and closed here, so the
 * view controller teardown leaves no client or database instance behind.
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

private fun createIosTopHeadlinesPresenter(): IosTopHeadlinesComposition {
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
    val presenter =
        TopHeadlinesPresenter(
            repository = OfflineFirstNewsRepository(remoteDataSource, localDataSource),
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
