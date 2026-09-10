package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
import pl.recipesforsoftware.signalbrief.ui.onboarding.rememberOnboardingPresenter

/**
 * Shared application shell for SignalBrief.
 *
 * Decides between the two-page onboarding flow and the main three-destination
 * app based on [onboardingCompleted]:
 * - `null`  -> the persisted value is still loading; a subtle loading indicator
 *              is shown to avoid an onboarding flash.
 * - `false` -> onboarding is shown.
 * - `true`  -> the host-provided destination content with a three-item bottom
 *              navigation bar (Headlines / Brief / Saved) is shown.
 *
 * The shell owns both navigation levels. Top-level navigation is exactly the
 * three [AppDestination] entries, kept in `rememberSaveable` with an explicit
 * [Saver]; it survives host recreation and defaults to
 * [AppDestination.Headlines]. Child navigation is a single nullable selected
 * article: when set, Article Details replaces the destination content and the
 * bottom bar disappears (details is a child screen, not a third tab), while
 * [currentDestination] keeps holding the originating destination so Back
 * returns to it exactly. Search is a child screen of Headlines and is tracked
 * by [isSearchVisible]; it also survives recreation through `rememberSaveable`.
 *
 * Topic Monitoring is a child of Search and can descend one level further into
 * Topic Matches for a selected [MonitoredTopic]; the selected topic survives
 * recreation through [SelectedMonitoredTopicSaver] and its matches are derived
 * reactively from the local headline cache.
 *
 * Navigation priority while the main app is visible:
 * 1. Topic Monitoring -> Topic Matches -> Article Details (when selected).
 * 2. Collections -> Collection Details -> Article Details (when selected).
 * 3. [selectedArticle] -> Article Details.
 * 4. [isSearchVisible] -> Search.
 * 5. [isSettingsVisible] -> Settings.
 * 6. [currentDestination] -> Headlines, Daily Brief, or Saved.
 *
 * Toolbar back and any host-integrated system back both funnel through the
 * same state clear, so there is one shared transition path and no back stack.
 * The selected article, collection, and monitored topic survive recreation
 * through [SelectedArticleSaver], [SelectedCollectionSaver], and
 * [SelectedMonitoredTopicSaver].
 *
 * The shell also installs the shared Coil image-loader singleton once for the
 * app composition root. Both "Skip" and "Start reading" funnel through an
 * [OnboardingCompletion] guard so [onCompleteOnboarding] fires at most once
 * per shell instance; the host persists the outcome itself.
 */
typealias TopHeadlinesContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onSearchClick: () -> Unit,
        onSettingsClick: () -> Unit,
    ) -> Unit

typealias SavedContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onCollectionsClick: () -> Unit,
    ) -> Unit

typealias CollectionsContent = @Composable (onBack: () -> Unit, onCollectionClick: (Collection) -> Unit) -> Unit

typealias CollectionDetailsContent =
    @Composable (collection: Collection, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit

typealias DailyBriefContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
    ) -> Unit

typealias SearchContent =
    @Composable (
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
    @Composable (
        article: Article,
        onBack: () -> Unit,
        onCollectionsClick: () -> Unit,
    ) -> Unit

typealias SettingsContent =
    @Composable (onBack: () -> Unit) -> Unit

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
        ) {
            InitializingContent()
        }

        if (onboardingCompleted == false) {
            val onboardingPresenter = rememberOnboardingPresenter()
            val completion = remember { OnboardingCompletion(onCompleteOnboarding) }

            OnboardingScreen(
                presenter = onboardingPresenter,
                onSkip = completion::complete,
                onComplete = completion::complete,
            )
        }

        if (onboardingCompleted == true) {
            SignalBriefMainContent(
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
@Suppress("LongMethod")
private fun SignalBriefMainContent(
    topHeadlinesContent: TopHeadlinesContent,
    dailyBriefContent: DailyBriefContent,
    savedContent: SavedContent,
    searchContent: SearchContent,
    articleDetailsContent: ArticleDetailsContent,
    collectionsContent: CollectionsContent,
    collectionDetailsContent: CollectionDetailsContent,
    topicMonitoringContent: TopicMonitoringContent,
    topicMatchesContent: TopicMatchesContent,
    settingsContent: SettingsContent,
    savedArticleCount: Int,
) {
    var currentDestination by rememberSaveable(stateSaver = AppDestinationSaver) {
        mutableStateOf(AppDestination.Headlines)
    }
    var selectedArticle by rememberSaveable(stateSaver = SelectedArticleSaver) {
        mutableStateOf<Article?>(null)
    }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var isTopicMonitoringVisible by rememberSaveable { mutableStateOf(false) }
    var isCollectionsVisible by rememberSaveable { mutableStateOf(false) }
    var isSettingsVisible by rememberSaveable { mutableStateOf(false) }
    var selectedCollection by rememberSaveable(stateSaver = SelectedCollectionSaver) {
        mutableStateOf<Collection?>(null)
    }
    var selectedMonitoredTopic by rememberSaveable(stateSaver = SelectedMonitoredTopicSaver) {
        mutableStateOf<MonitoredTopic?>(null)
    }
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    if (isTopicMonitoringVisible) {
        when {
            selectedMonitoredTopic == null -> {
                topicMonitoringContent(
                    { selectedMonitoredTopic = it },
                    { isTopicMonitoringVisible = false },
                )
            }

            selectedArticle != null -> {
                ArticleDetailsDestination(
                    articleDetailsContent,
                    requireNotNull(selectedArticle),
                    onBack = { selectedArticle = null },
                    onCollectionsClick = {
                        selectedArticle = null
                        selectedMonitoredTopic = null
                        isTopicMonitoringVisible = false
                        selectedCollection = null
                        isCollectionsVisible = true
                    },
                )
            }

            else -> {
                topicMatchesContent(
                    requireNotNull(selectedMonitoredTopic),
                    { selectedArticle = it },
                    { selectedMonitoredTopic = null },
                )
            }
        }
    } else if (isCollectionsVisible) {
        when {
            selectedCollection == null -> {
                collectionsContent(
                    { isCollectionsVisible = false },
                    { selectedCollection = it },
                )
            }

            selectedArticle != null -> {
                ArticleDetailsDestination(
                    articleDetailsContent,
                    requireNotNull(selectedArticle),
                    onBack = { selectedArticle = null },
                    onCollectionsClick = {
                        selectedCollection = null
                        isCollectionsVisible = true
                    },
                )
            }

            else -> {
                collectionDetailsContent(
                    requireNotNull(selectedCollection),
                    { selectedArticle = it },
                    { selectedCollection = null },
                )
            }
        }
    } else if (selectedArticle != null) {
        ArticleDetailsDestination(
            articleDetailsContent,
            requireNotNull(selectedArticle),
            onBack = { selectedArticle = null },
            onCollectionsClick = {
                selectedCollection = null
                isCollectionsVisible = true
            },
        )
    } else if (isSearchVisible) {
        searchContent(
            searchQuery,
            { searchQuery = it },
            { selectedArticle = it },
            { isTopicMonitoringVisible = true },
            { isSearchVisible = false },
        )
    } else if (isSettingsVisible) {
        settingsContent(
            { isSettingsVisible = false },
        )
    } else {
        SignalBriefDestinations(
            currentDestination = currentDestination,
            onDestinationChanged = { currentDestination = it },
            savedArticleCount = savedArticleCount,
            topHeadlinesContent = topHeadlinesContent,
            dailyBriefContent = dailyBriefContent,
            savedContent = savedContent,
            onArticleClick = { selectedArticle = it },
            onSearchClick = { isSearchVisible = true },
            onSettingsClick = { isSettingsVisible = true },
            onCollectionsClick = { isCollectionsVisible = true },
        )
    }
}

@Composable
private fun SignalBriefDestinations(
    currentDestination: AppDestination,
    onDestinationChanged: (AppDestination) -> Unit,
    savedArticleCount: Int,
    topHeadlinesContent: TopHeadlinesContent,
    dailyBriefContent: DailyBriefContent,
    savedContent: SavedContent,
    onArticleClick: (Article) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
) {
    val bottomBar: @Composable () -> Unit = {
        SignalBriefBottomBar(
            currentDestination = currentDestination,
            onNavigate = onDestinationChanged,
            savedArticleCount = savedArticleCount,
        )
    }

    when (currentDestination) {
        AppDestination.Headlines -> {
            topHeadlinesContent(
                bottomBar,
                onArticleClick,
                onSearchClick,
                onSettingsClick,
            )
        }

        AppDestination.DailyBrief -> {
            dailyBriefContent(bottomBar, onArticleClick)
        }

        AppDestination.Saved -> {
            savedContent(
                bottomBar,
                onArticleClick,
                onCollectionsClick,
            )
        }
    }
}

@Composable
private fun ArticleDetailsDestination(
    content: ArticleDetailsContent,
    article: Article,
    onBack: () -> Unit,
    onCollectionsClick: () -> Unit,
) {
    content(article, onBack, onCollectionsClick)
}

@Composable
private fun SignalBriefBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == AppDestination.Headlines,
            onClick = { onNavigate(AppDestination.Headlines) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Headlines,
                    contentDescription = null,
                )
            },
            label = { Text("Headlines") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
        NavigationBarItem(
            selected = currentDestination == AppDestination.DailyBrief,
            onClick = { onNavigate(AppDestination.DailyBrief) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Brief,
                    contentDescription = "Daily Brief",
                )
            },
            label = { Text("Brief") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
        SavedNavigationBarItem(currentDestination, onNavigate, savedArticleCount)
    }
}

@Composable
private fun RowScope.SavedNavigationBarItem(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBarItem(
        selected = currentDestination == AppDestination.Saved,
        onClick = { onNavigate(AppDestination.Saved) },
        icon = {
            BadgedBox(
                badge = {
                    savedCountBadgeLabel(savedArticleCount)?.let { label ->
                        Badge { Text(label) }
                    }
                },
            ) {
                Icon(
                    imageVector = NavigationIcons.Saved,
                    contentDescription = null,
                )
            }
        },
        label = { Text("Saved") },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    )
}

/** Returns the compact Saved navigation badge label, or `null` when it should be hidden. */
internal fun savedCountBadgeLabel(savedArticleCount: Int): String? =
    when {
        savedArticleCount <= 0 -> null
        savedArticleCount > MAX_SAVED_COUNT_BADGE -> "$MAX_SAVED_COUNT_BADGE+"
        else -> savedArticleCount.toString()
    }

private const val MAX_SAVED_COUNT_BADGE = 99

@Composable
private fun InitializingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Compose [Saver] that persists the selected [AppDestination] as its enum name.
 *
 * The enum is not JVM-serializable and must not assume Android-only save/restore
 * semantics; saving the stable name and restoring by lookup keeps the strategy
 * compatible with Compose Multiplatform.
 */
internal val AppDestinationSaver: Saver<AppDestination, String> =
    Saver(
        save = { destination -> destination.name },
        restore = { name -> AppDestination.entries.find { it.name == name } },
    )
