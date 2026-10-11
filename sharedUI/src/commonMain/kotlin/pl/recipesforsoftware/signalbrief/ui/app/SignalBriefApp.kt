package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingCompletion
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingScreen

typealias TopHeadlinesContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onSearchClick: () -> Unit,
        onSettingsClick: () -> Unit,
    ) -> Unit
typealias SavedContent = @Composable (
    bottomBar: @Composable () -> Unit,
    onArticleClick: (Article) -> Unit,
    onCollectionsClick: () -> Unit,
) -> Unit
typealias CollectionsContent = @Composable (onBack: () -> Unit, onCollectionClick: (Collection) -> Unit) -> Unit
typealias CollectionDetailsContent =
    @Composable (collection: Collection, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit
typealias DailyBriefContent =
    @Composable (bottomBar: @Composable () -> Unit, onArticleClick: (Article) -> Unit) -> Unit
typealias SearchContent = @Composable (
    initialQuery: String,
    onQueryChange: (String) -> Unit,
    onArticleClick: (Article) -> Unit,
    onOpenTopicMonitoring: () -> Unit,
    onBack: () -> Unit,
) -> Unit
typealias TopicMonitoringContent =
    @Composable (onOpenTopicMatches: (MonitoredTopic) -> Unit, onBack: () -> Unit) -> Unit
typealias TopicMatchesContent =
    @Composable (topic: MonitoredTopic, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit
typealias ArticleDetailsContent =
    @Composable (article: Article, onBack: () -> Unit, onCollectionsClick: () -> Unit) -> Unit
typealias SettingsContent = @Composable (onBack: () -> Unit) -> Unit

/** Shared application shell that selects onboarding or the Navigation 3 app host. */
@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    topHeadlinesContent: TopHeadlinesContent,
    savedContent: SavedContent,
    searchContent: SearchContent,
    articleDetailsContent: ArticleDetailsContent,
    dailyBriefContent: DailyBriefContent,
    collectionsContent: CollectionsContent = { _, _ -> },
    collectionDetailsContent: CollectionDetailsContent = { _, _, _ -> },
    topicMonitoringContent: TopicMonitoringContent = { _, _ -> },
    topicMatchesContent: TopicMatchesContent = { _, _, _ -> },
    settingsContent: SettingsContent = { _ -> },
    savedArticleCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    installSignalBriefImageLoader()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = onboardingCompleted == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) { InitializingContent() }
        if (onboardingCompleted == false) {
            var onboardingPageIndex by rememberSaveable { mutableStateOf(0) }
            val completion = remember { OnboardingCompletion(onCompleteOnboarding) }
            OnboardingScreen(
                pageIndex = onboardingPageIndex,
                onContinue = { onboardingPageIndex = 1 },
                onBack = { onboardingPageIndex = 0 },
                onSkip = completion::complete,
                onComplete = completion::complete,
            )
        }
        if (onboardingCompleted == true) {
            SignalBriefNavHost(
                topHeadlinesContent = topHeadlinesContent,
                dailyBriefContent = dailyBriefContent,
                savedContent = savedContent,
                searchContent = searchContent,
                articleDetailsContent = articleDetailsContent,
                collectionsContent = collectionsContent,
                collectionDetailsContent = collectionDetailsContent,
                topicMonitoringContent = topicMonitoringContent,
                topicMatchesContent = topicMatchesContent,
                settingsContent = settingsContent,
                savedArticleCount = savedArticleCount,
            )
        }
    }
}

@Composable
private fun InitializingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
