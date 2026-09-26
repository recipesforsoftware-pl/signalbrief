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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.compose.viewmodel.koinViewModel
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
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
        val themeViewModel: ThemeViewModel = koinViewModel()
        val isDarkMode by themeViewModel.isDarkMode.collectAsState()

        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val onboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()
        val mainViewModel: MainViewModel = koinViewModel()
        val savedArticleCount by mainViewModel.savedArticleCount.collectAsState(initial = 0)

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
                        onBack = onBack,
                        onManageCollections = onCollectionsClick,
                    )
                },
                collectionsContent = { onBack, onCollectionClick ->
                    CollectionsRoute(
                        onBack,
                        onCollectionClick,
                    )
                },
                collectionDetailsContent = { collection, onArticleClick, onBack ->
                    CollectionDetailsRoute(collection, onArticleClick, onBack)
                },
                topicMonitoringContent = { openMatches, onBack ->
                    TopicMonitoringRoute(
                        openMatches,
                        onBack,
                    )
                },
                topicMatchesContent = { topic, onArticleClick, onBack ->
                    TopicMatchesRoute(topic, onArticleClick, onBack)
                },
                settingsContent = { onBack ->
                    SettingsRoute(
                        onBack = onBack,
                    )
                },
                savedArticleCount = savedArticleCount,
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
}
