package com.recipesforsoftware.mvvm.ui.topheadline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.ui.app.SignalBriefApp
import com.recipesforsoftware.mvvm.ui.onboarding.OnboardingViewModel
import com.recipesforsoftware.mvvm.ui.theme.NewsAppTheme
import com.recipesforsoftware.mvvm.ui.theme.ThemeViewModel
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesScreen
import com.recipesforsoftware.mvvm.ui.topheadlines.hasActionableUrl
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopHeadlineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()

            // Keep a local optimistic copy so the UI switches immediately after
            // the user completes onboarding while DataStore propagates the value.
            var localOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

            NewsAppTheme(isDarkMode = isDarkMode) {
                val resolvedCompleted = localOnboardingCompleted ?: onboardingCompleted
                val uriHandler = LocalUriHandler.current
                val openArticle = rememberOpenArticleAction(uriHandler)

                SignalBriefApp(
                    onboardingCompleted = resolvedCompleted,
                    onCompleteOnboarding = {
                        localOnboardingCompleted = true
                        onboardingViewModel.completeOnboarding()
                    },
                    topHeadlinesContent = {
                        val viewModel: TopHeadlineViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()

                        TopHeadlinesScreen(
                            uiState = uiState,
                            onRefresh = viewModel::refresh,
                            onArticleClick = openArticle,
                            topBarActions = {
                                DarkModeMenu(
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = themeViewModel::toggleDarkMode,
                                )
                            },
                        )
                    },
                )
            }
        }
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
}
