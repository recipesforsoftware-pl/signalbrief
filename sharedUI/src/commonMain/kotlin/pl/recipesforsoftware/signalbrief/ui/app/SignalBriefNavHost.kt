package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.EntryProviderScope
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
fun SignalBriefNavHost(
    savedArticleCount: Int,
    entries: EntryProviderScope<NavKey>.(SignalBriefNavigationContext) -> Unit,
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
    val navigationContext =
        SignalBriefNavigationContext(
            openArticle = { article ->
                navigationArticleSnapshots = navigationArticleSnapshots + (article.url to article)
                navigationBackStack.add(SignalBriefRoute.ArticleDetails(article.url))
            },
            openCollection = { collection ->
                navigationCollectionSnapshots = navigationCollectionSnapshots + (collection.id to collection)
                navigationBackStack.add(SignalBriefRoute.CollectionDetails(collection.id))
            },
            openTopicMatches = { topic ->
                navigationMonitoredTopicSnapshots = navigationMonitoredTopicSnapshots + (topic.id to topic)
                navigationBackStack.add(SignalBriefRoute.TopicMatches(topic.id))
            },
            openSearch = { navigationBackStack.add(SignalBriefRoute.Search) },
            openSettings = { navigationBackStack.add(SignalBriefRoute.Settings) },
            openCollections = { navigationBackStack.add(SignalBriefRoute.Collections) },
            openTopicMonitoring = { navigationBackStack.add(SignalBriefRoute.TopicMonitoring) },
            back = navigationBackStack::removeLastOrNull,
            searchQuery = { searchQuery },
            onSearchQueryChange = { searchQuery = it },
            articleFor = { navigationArticleSnapshots[it] },
            collectionFor = { navigationCollectionSnapshots[it] },
            topicFor = { navigationMonitoredTopicSnapshots[it] },
            bottomBarFor = { bottomBar(it, navigationBackStack, savedArticleCount) },
        )
    NavDisplay(
        backStack = navigationBackStack,
        entryProvider =
            entryProvider { entries(navigationContext) },
    )
}

class SignalBriefNavigationContext internal constructor(
    val openArticle: (Article) -> Unit,
    val openCollection: (Collection) -> Unit,
    val openTopicMatches: (MonitoredTopic) -> Unit,
    val openSearch: () -> Unit,
    val openSettings: () -> Unit,
    val openCollections: () -> Unit,
    val openTopicMonitoring: () -> Unit,
    val back: () -> Unit,
    val searchQuery: () -> String,
    val onSearchQueryChange: (String) -> Unit,
    val articleFor: (String) -> Article?,
    val collectionFor: (String) -> Collection?,
    val topicFor: (String) -> MonitoredTopic?,
    val bottomBarFor: (SignalBriefRoute) -> @Composable () -> Unit,
)

private fun <T> Map<String, T>.prunedTo(activeIds: Set<String>): Map<String, T> {
    val pruned = filterKeys(activeIds::contains)
    return if (pruned == this) this else pruned
}

@Composable
fun InvalidDestination(
    identifier: String,
    back: () -> Unit,
) {
    LaunchedEffect(identifier) { back() }
}

private fun bottomBar(
    currentDestination: SignalBriefRoute,
    navigationBackStack: NavBackStack<NavKey>,
    savedArticleCount: Int,
): @Composable () -> Unit =
    {
        SignalBriefBottomBar(currentDestination, navigationBackStack::navigateToTopLevelRoute, savedArticleCount)
    }
