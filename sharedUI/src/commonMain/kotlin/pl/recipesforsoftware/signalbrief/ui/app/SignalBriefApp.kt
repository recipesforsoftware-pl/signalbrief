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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.navigation.SignalBriefRoute
import pl.recipesforsoftware.signalbrief.ui.navigation.navigateToTopLevelRoute
import pl.recipesforsoftware.signalbrief.ui.navigation.signalBriefNavigationSavedStateConfiguration
import pl.recipesforsoftware.signalbrief.ui.navigation.signalBriefStartRoute
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingCompletion
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingScreen

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
 * The shell owns both navigation levels. The Navigation 3 back stack contains
 * the three top-level routes plus the migrated Search and Article Details
 * child entries. It survives host recreation and defaults to
 * [SignalBriefRoute.Headlines], while a child Back action returns to the
 * preceding entry. Remaining child flows are temporarily state-based.
 *
 * Topic Monitoring is a child of Search and can descend one level further into
 * Topic Matches for a selected [MonitoredTopic]; the selected topic survives
 * recreation through [SelectedMonitoredTopicSaver] and its matches are derived
 * reactively from the local headline cache.
 *
 * Navigation priority while the main app is visible:
 * 1. Topic Monitoring -> Topic Matches -> Article Details (when selected).
 * 2. Collections -> Collection Details -> Article Details (when selected).
 * 3. legacy selected Article -> legacy Article Details.
 * 4. [isSettingsVisible] -> Settings.
 * 5. Navigation 3 route -> Headlines, Daily Brief, Saved, Search, or Details.
 *
 * Toolbar back and any host-integrated system back both funnel through the
 * same state clear for legacy child navigation. The legacy selected article,
 * collection, and monitored topic survive recreation
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
    val navigationBackStack =
        rememberNavBackStack(
            signalBriefNavigationSavedStateConfiguration,
            signalBriefStartRoute,
        )
    var legacySelectedArticle by rememberSaveable(stateSaver = SelectedArticleSaver) {
        mutableStateOf<Article?>(null)
    }
    var navigationArticleSnapshot by rememberSaveable(stateSaver = SelectedArticleSaver) {
        mutableStateOf<Article?>(null)
    }
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

            legacySelectedArticle != null -> {
                ArticleDetailsDestination(
                    articleDetailsContent,
                    requireNotNull(legacySelectedArticle),
                    onBack = { legacySelectedArticle = null },
                    onCollectionsClick = {
                        legacySelectedArticle = null
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
                    { legacySelectedArticle = it },
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

            legacySelectedArticle != null -> {
                ArticleDetailsDestination(
                    articleDetailsContent,
                    requireNotNull(legacySelectedArticle),
                    onBack = { legacySelectedArticle = null },
                    onCollectionsClick = {
                        selectedCollection = null
                        isCollectionsVisible = true
                    },
                )
            }

            else -> {
                collectionDetailsContent(
                    requireNotNull(selectedCollection),
                    { legacySelectedArticle = it },
                    { selectedCollection = null },
                )
            }
        }
    } else if (legacySelectedArticle != null) {
        ArticleDetailsDestination(
            articleDetailsContent,
            requireNotNull(legacySelectedArticle),
            onBack = { legacySelectedArticle = null },
            onCollectionsClick = {
                selectedCollection = null
                isCollectionsVisible = true
            },
        )
    } else if (isSettingsVisible) {
        settingsContent(
            { isSettingsVisible = false },
        )
    } else {
        SignalBriefDestinations(
            navigationBackStack = navigationBackStack,
            savedArticleCount = savedArticleCount,
            topHeadlinesContent = topHeadlinesContent,
            dailyBriefContent = dailyBriefContent,
            savedContent = savedContent,
            searchContent = searchContent,
            articleDetailsContent = articleDetailsContent,
            navigationArticleSnapshot = navigationArticleSnapshot,
            onArticleClick = { article ->
                navigationArticleSnapshot = article
                navigationBackStack.add(SignalBriefRoute.ArticleDetails(article.url))
            },
            onSearchClick = { navigationBackStack.add(SignalBriefRoute.Search) },
            onSettingsClick = { isSettingsVisible = true },
            onCollectionsClick = { isCollectionsVisible = true },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onOpenTopicMonitoring = { isTopicMonitoringVisible = true },
            onDetailsCollectionsClick = {
                selectedCollection = null
                isCollectionsVisible = true
            },
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun SignalBriefDestinations(
    navigationBackStack: NavBackStack<NavKey>,
    savedArticleCount: Int,
    topHeadlinesContent: TopHeadlinesContent,
    dailyBriefContent: DailyBriefContent,
    savedContent: SavedContent,
    searchContent: SearchContent,
    articleDetailsContent: ArticleDetailsContent,
    navigationArticleSnapshot: Article?,
    onArticleClick: (Article) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenTopicMonitoring: () -> Unit,
    onDetailsCollectionsClick: () -> Unit,
) {
    NavDisplay(
        backStack = navigationBackStack,
        entryProvider =
            entryProvider {
                entry<SignalBriefRoute.Headlines> {
                    topHeadlinesContent(
                        topLevelBottomBar(
                            currentDestination = SignalBriefRoute.Headlines,
                            navigationBackStack = navigationBackStack,
                            savedArticleCount = savedArticleCount,
                        ),
                        onArticleClick,
                        onSearchClick,
                        onSettingsClick,
                    )
                }
                entry<SignalBriefRoute.DailyBrief> {
                    dailyBriefContent(
                        topLevelBottomBar(
                            currentDestination = SignalBriefRoute.DailyBrief,
                            navigationBackStack = navigationBackStack,
                            savedArticleCount = savedArticleCount,
                        ),
                        onArticleClick,
                    )
                }
                entry<SignalBriefRoute.Saved> {
                    savedContent(
                        topLevelBottomBar(
                            currentDestination = SignalBriefRoute.Saved,
                            navigationBackStack = navigationBackStack,
                            savedArticleCount = savedArticleCount,
                        ),
                        onArticleClick,
                        onCollectionsClick,
                    )
                }
                entry<SignalBriefRoute.Search> {
                    searchContent(
                        searchQuery,
                        onSearchQueryChange,
                        onArticleClick,
                        onOpenTopicMonitoring,
                        { navigationBackStack.removeLastOrNull() },
                    )
                }
                entry<SignalBriefRoute.ArticleDetails> { route ->
                    val article = navigationArticleSnapshot
                    if (article?.url == route.articleUrl) {
                        ArticleDetailsDestination(
                            articleDetailsContent,
                            article,
                            onBack = { navigationBackStack.removeLastOrNull() },
                            onCollectionsClick = onDetailsCollectionsClick,
                        )
                    } else {
                        LaunchedEffect(route.articleUrl) {
                            navigationBackStack.removeLastOrNull()
                        }
                    }
                }
            },
    )
}

private fun topLevelBottomBar(
    currentDestination: SignalBriefRoute,
    navigationBackStack: NavBackStack<NavKey>,
    savedArticleCount: Int,
): @Composable () -> Unit =
    {
        SignalBriefBottomBar(
            currentDestination = currentDestination,
            onNavigate = { destination ->
                navigationBackStack.navigateToTopLevelRoute(destination)
            },
            savedArticleCount = savedArticleCount,
        )
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
    currentDestination: SignalBriefRoute,
    onNavigate: (SignalBriefRoute) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == SignalBriefRoute.Headlines,
            onClick = { onNavigate(SignalBriefRoute.Headlines) },
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
            selected = currentDestination == SignalBriefRoute.DailyBrief,
            onClick = { onNavigate(SignalBriefRoute.DailyBrief) },
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
    currentDestination: SignalBriefRoute,
    onNavigate: (SignalBriefRoute) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBarItem(
        selected = currentDestination == SignalBriefRoute.Saved,
        onClick = { onNavigate(SignalBriefRoute.Saved) },
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
