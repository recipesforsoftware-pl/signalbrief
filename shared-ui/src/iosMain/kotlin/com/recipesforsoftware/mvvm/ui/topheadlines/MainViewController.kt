package com.recipesforsoftware.mvvm.ui.topheadlines

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.recipesforsoftware.mvvm.data.remote.KtorNewsRepository
import com.recipesforsoftware.mvvm.data.remote.NewsApiConfig
import com.recipesforsoftware.mvvm.data.remote.createHttpClient
import platform.Foundation.NSBundle
import platform.UIKit.UIViewController

/**
 * iOS composition root, invoked from SwiftUI as `MainViewControllerKt.mainViewController()`.
 *
 * Assembles the shared data layer and [TopHeadlinesPresenter] without any
 * platform dependency injection framework, mirrors the Android wiring
 * (`NewsApiConfig` + [createHttpClient] + [KtorNewsRepository]), and lets the
 * screen live entirely in [SignalBriefTheme].
 */
fun mainViewController(): UIViewController =
    ComposeUIViewController {
        SignalBriefTheme {
            TopHeadlinesRoute()
        }
    }

@Composable
private fun TopHeadlinesRoute() {
    val presenter = remember { createIosTopHeadlinesPresenter() }
    val uiState by presenter.uiState.collectAsState()

    DisposableEffect(presenter) {
        onDispose { presenter.dispose() }
    }

    TopHeadlinesScreen(
        uiState = uiState,
        onRefresh = presenter::refresh,
    )
}

private fun createIosTopHeadlinesPresenter(): TopHeadlinesPresenter {
    val apiKey =
        readNewsApiKeyFromBundle()
            ?: error(
                "NEWS_API_KEY is missing or empty. " +
                    "Configure iosApp/Secrets.xcconfig (copy Secrets.example.xcconfig) " +
                    "and rebuild the app.",
            )
    val config = NewsApiConfig(apiKey = apiKey, baseUrl = "https://newsapi.org/v2/")
    val repository = KtorNewsRepository(client = createHttpClient(config))
    return TopHeadlinesPresenter(repository = repository)
}

/**
 * Reads the NewsAPI key injected into Info.plist by the build via
 * `NEWS_API_KEY = $(NEWS_API_KEY)` from the git-ignored `Secrets.xcconfig`.
 */
private fun readNewsApiKeyFromBundle(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("NEWS_API_KEY") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
