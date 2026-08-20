package pl.recipesforsoftware.signalbrief.ui.main

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DarkModeMenu
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()

            // Keep system-bar icon appearance in sync with SignalBrief's own dark
            // mode preference instead of the Android system theme, so the icons
            // always contrast with the app's edge-to-edge, transparent bars.
            SideEffect {
                val barStyle =
                    if (isDarkMode) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            // Keep a local optimistic copy so the UI switches immediately after
            // the user completes onboarding while DataStore propagates the value.
            var localOnboardingCompleted by remember { mutableStateOf<Boolean?>(null) }

            SignalBriefAndroidTheme(isDarkMode = isDarkMode) {
                val resolvedCompleted = localOnboardingCompleted ?: onboardingCompleted
                val uriHandler = LocalUriHandler.current
                val openArticle = rememberOpenArticleAction(uriHandler)

                SignalBriefApp(
                    onboardingCompleted = resolvedCompleted,
                    onCompleteOnboarding = {
                        localOnboardingCompleted = true
                        onboardingViewModel.completeOnboarding()
                    },
                    topHeadlinesContent = { bottomBar ->
                        val viewModel: TopHeadlinesViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()

                        TopHeadlinesScreen(
                            uiState = uiState,
                            onRefresh = viewModel::refresh,
                            onArticleClick = openArticle,
                            onBookmarkClick = viewModel::toggleBookmark,
                            topBarActions = {
                                DarkModeMenu(
                                    isDarkMode = isDarkMode,
                                    onToggleDarkMode = themeViewModel::toggleDarkMode,
                                )
                            },
                            bottomBar = bottomBar,
                        )
                    },
                    savedContent = { bottomBar ->
                        val viewModel: SavedArticlesViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsState()

                        SavedArticlesScreen(
                            uiState = uiState,
                            onArticleClick = openArticle,
                            onRemoveClick = { viewModel.removeArticle(it.url) },
                            bottomBar = bottomBar,
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
