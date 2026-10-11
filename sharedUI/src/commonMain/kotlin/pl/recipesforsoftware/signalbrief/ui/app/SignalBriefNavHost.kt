package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.navigation.SignalBriefRoute
import pl.recipesforsoftware.signalbrief.ui.navigation.navigateToTopLevelRoute
import pl.recipesforsoftware.signalbrief.ui.navigation.signalBriefNavigationSavedStateConfiguration
import pl.recipesforsoftware.signalbrief.ui.navigation.signalBriefStartRoute

/** The sole Navigation 3 router, including bounded saved rendering payloads. */
@Composable
@Suppress("LongMethod")
internal fun SignalBriefNavHost(
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
    val navigationBackStack = rememberNavBackStack(signalBriefNavigationSavedStateConfiguration, signalBriefStartRoute)
    var navigationArticleSnapshots by rememberSaveable(stateSaver = NavigationArticleSnapshotsSaver) {
        mutableStateOf<Map<String, Article>>(emptyMap())
    }
    var navigationCollectionSnapshots by rememberSaveable(stateSaver = NavigationCollectionSnapshotsSaver) {
        mutableStateOf<Map<String, Collection>>(emptyMap())
    }
    var navigationMonitoredTopicSnapshots by rememberSaveable(stateSaver = NavigationMonitoredTopicSnapshotsSaver) {
        mutableStateOf<Map<String, MonitoredTopic>>(emptyMap())
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val activeArticleUrls =
        navigationBackStack.filterIsInstance<SignalBriefRoute.ArticleDetails>().map { it.articleUrl }.toSet()
    val activeCollectionIds =
        navigationBackStack.filterIsInstance<SignalBriefRoute.CollectionDetails>().map { it.collectionId }.toSet()
    val activeTopicIds =
        navigationBackStack.filterIsInstance<SignalBriefRoute.TopicMatches>().map { it.topicId }.toSet()
    LaunchedEffect(activeArticleUrls) {
        navigationArticleSnapshots = navigationArticleSnapshots.prunedTo(activeArticleUrls)
    }
    LaunchedEffect(activeCollectionIds) {
        navigationCollectionSnapshots = navigationCollectionSnapshots.prunedTo(activeCollectionIds)
    }
    LaunchedEffect(activeTopicIds) {
        navigationMonitoredTopicSnapshots = navigationMonitoredTopicSnapshots.prunedTo(activeTopicIds)
    }
    val onArticleClick: (Article) -> Unit = { article ->
        navigationArticleSnapshots = navigationArticleSnapshots + (article.url to article)
        navigationBackStack.add(SignalBriefRoute.ArticleDetails(article.url))
    }
    NavDisplay(
        backStack = navigationBackStack,
        entryProvider =
            entryProvider {
                entry<SignalBriefRoute.Headlines> {
                    topHeadlinesContent(
                        bottomBar(SignalBriefRoute.Headlines, navigationBackStack, savedArticleCount),
                        onArticleClick,
                        { navigationBackStack.add(SignalBriefRoute.Search) },
                        { navigationBackStack.add(SignalBriefRoute.Settings) },
                    )
                }
                entry<SignalBriefRoute.DailyBrief> {
                    dailyBriefContent(
                        bottomBar(SignalBriefRoute.DailyBrief, navigationBackStack, savedArticleCount),
                        onArticleClick,
                    )
                }
                entry<SignalBriefRoute.Saved> {
                    savedContent(
                        bottomBar(SignalBriefRoute.Saved, navigationBackStack, savedArticleCount),
                        onArticleClick,
                        { navigationBackStack.add(SignalBriefRoute.Collections) },
                    )
                }
                entry<SignalBriefRoute.Search> {
                    searchContent(
                        searchQuery,
                        { searchQuery = it },
                        onArticleClick,
                        { navigationBackStack.add(SignalBriefRoute.TopicMonitoring) },
                        navigationBackStack::removeLastOrNull,
                    )
                }
                entry<SignalBriefRoute.Settings> { settingsContent(navigationBackStack::removeLastOrNull) }
                entry<SignalBriefRoute.TopicMonitoring> {
                    topicMonitoringContent({ topic ->
                        navigationMonitoredTopicSnapshots = navigationMonitoredTopicSnapshots + (topic.id to topic)
                        navigationBackStack.add(SignalBriefRoute.TopicMatches(topic.id))
                    }, navigationBackStack::removeLastOrNull)
                }
                entry<SignalBriefRoute.TopicMatches> { route ->
                    navigationMonitoredTopicSnapshots[route.topicId]?.let { topic ->
                        topicMatchesContent(topic, onArticleClick, navigationBackStack::removeLastOrNull)
                    }
                        ?: InvalidDestination(route.topicId, navigationBackStack)
                }
                entry<SignalBriefRoute.ArticleDetails> { route ->
                    navigationArticleSnapshots[route.articleUrl]?.let { article ->
                        articleDetailsContent(
                            article,
                            navigationBackStack::removeLastOrNull,
                            { navigationBackStack.add(SignalBriefRoute.Collections) },
                        )
                    }
                        ?: InvalidDestination(route.articleUrl, navigationBackStack)
                }
                entry<SignalBriefRoute.Collections> {
                    collectionsContent(navigationBackStack::removeLastOrNull) { collection ->
                        navigationCollectionSnapshots = navigationCollectionSnapshots + (collection.id to collection)
                        navigationBackStack.add(SignalBriefRoute.CollectionDetails(collection.id))
                    }
                }
                entry<SignalBriefRoute.CollectionDetails> { route ->
                    navigationCollectionSnapshots[route.collectionId]?.let { collection ->
                        collectionDetailsContent(collection, onArticleClick, navigationBackStack::removeLastOrNull)
                    }
                        ?: InvalidDestination(route.collectionId, navigationBackStack)
                }
            },
    )
}

private fun <T> Map<String, T>.prunedTo(activeIds: Set<String>): Map<String, T> {
    val pruned = filterKeys(activeIds::contains)
    return if (pruned == this) this else pruned
}

@Composable
private fun InvalidDestination(
    identifier: String,
    navigationBackStack: NavBackStack<NavKey>,
) {
    LaunchedEffect(identifier) { navigationBackStack.removeLastOrNull() }
}

private fun bottomBar(
    currentDestination: SignalBriefRoute,
    navigationBackStack: NavBackStack<NavKey>,
    savedArticleCount: Int,
): @Composable () -> Unit =
    {
        SignalBriefBottomBar(currentDestination, navigationBackStack::navigateToTopLevelRoute, savedArticleCount)
    }
