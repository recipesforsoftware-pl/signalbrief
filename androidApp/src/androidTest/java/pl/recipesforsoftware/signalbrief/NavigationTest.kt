package pl.recipesforsoftware.signalbrief

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_brief
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_saved
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.daily_brief_intro
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.saved_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.saved_open_collections
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.saved_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_monitored_topics
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_offline_section
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_remove
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_save
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_search
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_settings
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_matches_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_matches_no_matches_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_title
import pl.recipesforsoftware.signalbrief.ui.app.InvalidDestination
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefNavHost
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefNavigationContext
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefUiState
import pl.recipesforsoftware.signalbrief.ui.navigation.SignalBriefRoute
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesUiState
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchUiState
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesUiState
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesUiState
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiState

private fun resourceString(resource: StringResource): String = runBlocking { getString(resource) }

class NavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeArticles =
        listOf(
            Article(
                title = "Test Article",
                description = "Description",
                url = "https://example.com/1",
                imageUrl = null,
                source = Source(id = "src1", name = "Source 1"),
            ),
        )

    @Composable
    private fun TestDetailsContent(
        article: Article,
        onBack: () -> Unit,
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Text(article.title.orEmpty())
    }

    private fun setContent(
        isDarkMode: Boolean = false,
        headlineArticles: List<Article> = fakeArticles,
        savedArticles: List<Article> = emptyList(),
        searchUiState: SearchUiState = SearchUiState.Idle,
        articleDetailsRoute: @Composable (article: Article, onBack: () -> Unit, onCollectionsClick: () -> Unit) -> Unit =
            { article, onBack, _ -> TestDetailsContent(article, onBack) },
        collectionsRoute: @Composable (onBack: () -> Unit, onCollectionClick: (Collection) -> Unit) -> Unit =
            { onBack, _ ->
                TextButton(onClick = onBack) { Text("Collections back") }
                Text("Collections")
            },
        collectionDetailsRoute: @Composable (collection: Collection, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit =
            { collection, _, onBack ->
                TextButton(onClick = onBack) { Text("Collection details back") }
                Text("Collection details ${collection.id}: ${collection.name}")
            },
        topicMonitoringRoute: @Composable (onOpenTopicMatches: (MonitoredTopic) -> Unit, onBack: () -> Unit) -> Unit =
            { openMatches, back ->
                TopicMonitoringScreen(
                    uiState = TopicMonitoringUiState(topics = listOf(MonitoredTopic("1", "Kotlin"))),
                    onOpenCreateEditor = {},
                    onOpenRenameEditor = {},
                    onUpdateEditorQuery = {},
                    onConfirmEditor = {},
                    onDismissEditor = {},
                    onOpenDeleteConfirmation = {},
                    onConfirmDelete = {},
                    onDismissDeleteConfirmation = {},
                    onDismissError = {},
                    onOpenTopicMatches = openMatches,
                    onBack = back,
                )
            },
        topicMatchesRoute: @Composable (topic: MonitoredTopic, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit =
            { topic, onArticleClick, onBack ->
                TopicMatchesScreen(
                    uiState =
                        TopicMatchesUiState.NoMatches(
                            topic = MonitoredTopic(topic.id, topic.query),
                        ),
                    onArticleClick = onArticleClick,
                    onBookmarkClick = {},
                    onBack = onBack,
                )
            },
        restorationTester: StateRestorationTester? = null,
    ) {
        val content: @Composable () -> Unit = {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                TestSignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    headlinesRoute = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                        TopHeadlinesScreen(
                            uiState =
                                TopHeadlinesUiState.Success(
                                    headlineArticles,
                                    FeedSource.NETWORK,
                                ),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onSearchClick = onSearchClick,
                            onSettingsClick = onSettingsClick,
                            bottomBar = bottomBar,
                        )
                    },
                    savedRoute = { bottomBar, onArticleClick, onCollectionsClick ->
                        SavedArticlesScreen(
                            uiState =
                                if (savedArticles.isEmpty()) {
                                    SavedArticlesUiState.Empty
                                } else {
                                    SavedArticlesUiState.Content(savedArticles)
                                },
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            onCollectionsClick = onCollectionsClick,
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefRoute = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(headlineArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchRoute = { initialQuery, onQueryChange, onArticleClick, monitoring, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState = searchUiState,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onOpenTopicMonitoring = monitoring,
                            onBack = onBack,
                        )
                    },
                    articleDetailsRoute = articleDetailsRoute,
                    collectionsRoute = collectionsRoute,
                    collectionDetailsRoute = collectionDetailsRoute,
                    topicMonitoringRoute = topicMonitoringRoute,
                    topicMatchesRoute = topicMatchesRoute,
                    settingsRoute = { onBack ->
                        SettingsScreen(
                            uiState = SettingsUiState(downloadedHeadlineCount = 0),
                            onBack = onBack,
                            onClearDownloadedHeadlines = {},
                        )
                    },
                )
            }
        }
        if (restorationTester == null) {
            composeTestRule.setContent(content)
        } else {
            restorationTester.setContent(content)
        }
    }

    @Test
    fun searchTopicMonitoringBackPreservesQuery() {
        setContent()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Kotlin")
        composeTestRule.onNodeWithText(resourceString(Res.string.search_monitored_topics)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_monitoring_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.topic_monitoring_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertTextEquals("Kotlin")
    }

    @Test
    fun searchTopicMatchesRoundTripPreservesQuery() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Android")
        composeTestRule.onNodeWithText(resourceString(Res.string.search_monitored_topics)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_monitoring_title)).assertIsDisplayed()

        composeTestRule.onNodeWithText("Kotlin").performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_matches_no_matches_title)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.topic_matches_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_monitoring_title)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.topic_monitoring_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertTextEquals("Android")
    }

    @Test
    fun topicMatchesArticleDetailsBackReturnsToTopicMatches() {
        setContent(
            topicMatchesRoute = { topic, onArticleClick, onBack ->
                TopicMatchesScreen(
                    uiState =
                        TopicMatchesUiState.Content(
                            topic = topic,
                            articles = fakeArticles,
                            savedUrls = emptySet(),
                        ),
                    onArticleClick = onArticleClick,
                    onBookmarkClick = {},
                    onBack = onBack,
                )
            },
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_monitored_topics)).performClick()
        composeTestRule.onNodeWithText("Kotlin").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_matches_no_matches_title)).assertDoesNotExist()
    }

    @Test
    fun topicArticleCollectionsStackSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(
            restorationTester = restorationTester,
            articleDetailsRoute = { article, onBack, onCollectionsClick ->
                Text("Topic details ${article.title}")
                TextButton(onClick = onBack) { Text("Article details back") }
                TextButton(onClick = onCollectionsClick) { Text("Manage collections") }
            },
            collectionsRoute = { onBack, _ ->
                Text("Collections")
                TextButton(onClick = onBack) { Text("Collections back") }
            },
            topicMatchesRoute = { topic, onArticleClick, onBack ->
                TopicMatchesScreen(
                    uiState = TopicMatchesUiState.Content(topic, fakeArticles, emptySet()),
                    onArticleClick = onArticleClick,
                    onBookmarkClick = {},
                    onBack = onBack,
                )
            },
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Topic query")
        composeTestRule.onNodeWithText(resourceString(Res.string.search_monitored_topics)).performClick()
        composeTestRule.onNodeWithText("Kotlin").performClick()
        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Manage collections").performClick()
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collections back").performClick()
        composeTestRule.onNodeWithText("Topic details Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Article details back").performClick()
        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.topic_matches_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.topic_monitoring_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.topic_monitoring_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertTextEquals("Topic query")
    }

    @Test
    fun bottomNavIsHiddenOnTopicRoutes() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_monitored_topics)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.topic_monitoring_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()

        composeTestRule.onNodeWithText("Kotlin").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.topic_matches_no_matches_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()
    }

    @Test
    fun headlinesIsDefaultDestination() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertIsDisplayed()
    }

    @Test
    fun tappingSavedShowsSavedDestination() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.saved_empty_title)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(resourceString(Res.string.saved_empty_title))
            .assertIsDisplayed()
    }

    @Test
    fun tappingHeadlinesReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_empty_title)).assertIsDisplayed()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
    }

    @Test
    fun exactlyThreeRealTopLevelDestinationsExist() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_brief)).assertIsDisplayed()

        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
        composeTestRule.onNodeWithText("Monitor").assertDoesNotExist()
        composeTestRule.onNodeWithText("Collections").assertDoesNotExist()
    }

    @Test
    fun dailyBriefOpensAndDetailsBackReturnsToBrief() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_brief)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_intro)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_intro)).assertIsDisplayed()
    }

    @Test
    fun savedDestinationSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TestSignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    headlinesRoute = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                        TopHeadlinesScreen(
                            uiState =
                                TopHeadlinesUiState.Success(
                                    fakeArticles,
                                    FeedSource.NETWORK,
                                ),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onSearchClick = onSearchClick,
                            onSettingsClick = onSettingsClick,
                            bottomBar = bottomBar,
                        )
                    },
                    savedRoute = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefRoute = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchRoute = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState = SearchUiState.Idle,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onOpenTopicMonitoring = {},
                            onBack = onBack,
                        )
                    },
                    articleDetailsRoute = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_empty_title)).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(resourceString(Res.string.saved_empty_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_empty_title)).assertIsDisplayed()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).performClick()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    @Test
    fun dailyBriefDestinationSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TestSignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    headlinesRoute = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                        TopHeadlinesScreen(
                            uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onSearchClick = onSearchClick,
                            onSettingsClick = onSettingsClick,
                            bottomBar = bottomBar,
                        )
                    },
                    savedRoute = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefRoute = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchRoute = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState = SearchUiState.Idle,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onOpenTopicMonitoring = {},
                            onBack = onBack,
                        )
                    },
                    articleDetailsRoute = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_brief)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_intro)).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_intro)).assertIsDisplayed()
    }

    @Test
    fun headlinesArticleTapOpensDetails() {
        setContent()

        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun detailsBackReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
    }

    @Test
    fun savedArticleTapOpensDetails() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun detailsBackReturnsToSaved() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
    }

    @Test
    fun detailsCollectionsBackReturnsToSameArticle() {
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(
            restorationTester = restorationTester,
            articleDetailsRoute = { article, onBack, onCollectionsClick ->
                TextButton(onClick = onBack) { Text("Back") }
                Text(article.title.orEmpty())
                TextButton(onClick = onCollectionsClick) { Text("Manage collections") }
            },
        )

        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Manage collections").performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collections back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage collections").assertIsDisplayed()
    }

    @Test
    fun savedCollectionsBackReturnsToSaved() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collections back").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.saved_title)).assertIsDisplayed()
    }

    @Test
    fun collectionsDetailsBackReturnsToCollections() {
        val collection = Collection("42", "Reading")
        setContent(
            collectionsRoute = { onBack, onCollectionClick ->
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        composeTestRule.onNodeWithText("Open Reading").performClick()
        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collection details back").performClick()

        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Reading").assertIsDisplayed()
    }

    @Test
    fun collectionDetailsArticleBackRestoresSameCollection() {
        val collection = Collection("42", "Reading")
        setContent(
            articleDetailsRoute = { article, onBack, _ ->
                TextButton(onClick = onBack) { Text("Article details back") }
                Text("Article details: ${article.url}")
            },
            collectionsRoute = { onBack, onCollectionClick ->
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
            collectionDetailsRoute = { selected, onArticleClick, onBack ->
                TextButton(onClick = onBack) { Text("Collection details back") }
                Text("Collection details ${selected.id}: ${selected.name}")
                TextButton(onClick = { onArticleClick(fakeArticles.single()) }) { Text("Open member article") }
            },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        composeTestRule.onNodeWithText("Open Reading").performClick()
        composeTestRule.onNodeWithText("Open member article").performClick()
        composeTestRule.onNodeWithText("Article details: https://example.com/1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Article details back").performClick()

        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open member article").assertIsDisplayed()
    }

    @Test
    fun collectionsAndCollectionDetailsHideBottomNav() {
        val collection = Collection("42", "Reading")
        setContent(
            collectionsRoute = { _, onCollectionClick ->
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()

        composeTestRule.onNodeWithText("Open Reading").performClick()
        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()
    }

    @Test
    fun collectionDetailsAndArticleRoundTripSurviveStateRestoration() {
        val collection = Collection("42", "Reading")
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(
            restorationTester = restorationTester,
            articleDetailsRoute = { article, onBack, _ ->
                TextButton(onClick = onBack) { Text("Article details back") }
                Text("Article details: ${article.url}")
            },
            collectionsRoute = { _, onCollectionClick ->
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
            collectionDetailsRoute = { selected, onArticleClick, _ ->
                Text("Collection details ${selected.id}: ${selected.name}")
                TextButton(onClick = { onArticleClick(fakeArticles.single()) }) { Text("Open member article") }
            },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()

        composeTestRule.onNodeWithText("Open Reading").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()

        composeTestRule.onNodeWithText("Open member article").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Article details: https://example.com/1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Article details back").performClick()
        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()
    }

    @Test
    fun nestedArticleDetailsKeepsEarlierArticlePayload() {
        val articleA = fakeArticles.single().copy(title = "Article A", url = "https://example.com/a")
        val articleB = fakeArticles.single().copy(title = "Article B", url = "https://example.com/b")
        val collectionX = Collection("x", "Collection X")
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(
            restorationTester = restorationTester,
            headlineArticles = listOf(articleA),
            articleDetailsRoute = { article, onBack, onCollectionsClick ->
                Text("Details ${article.title}")
                TextButton(onClick = onBack) { Text("Article details back") }
                TextButton(onClick = onCollectionsClick) { Text("Manage collections") }
            },
            collectionsRoute = { onBack, onCollectionClick ->
                Text("Collections")
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(onClick = { onCollectionClick(collectionX) }) { Text("Open Collection X") }
            },
            collectionDetailsRoute = { collection, onArticleClick, onBack ->
                Text("Details ${collection.name}")
                TextButton(onClick = onBack) { Text("Collection details back") }
                TextButton(onClick = { onArticleClick(articleB) }) { Text("Open Article B") }
            },
        )

        composeTestRule.onNodeWithText("Article A").performClick()
        composeTestRule.onNodeWithText("Manage collections").performClick()
        composeTestRule.onNodeWithText("Open Collection X").performClick()
        composeTestRule.onNodeWithText("Open Article B").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Details Article B").assertIsDisplayed()

        composeTestRule.onNodeWithText("Article details back").performClick()
        composeTestRule.onNodeWithText("Details Collection X").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collection details back").performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collections back").performClick()
        composeTestRule.onNodeWithText("Details Article A").assertIsDisplayed()
    }

    @Test
    fun nestedCollectionDetailsKeepsEarlierCollectionPayload() {
        val articleB = fakeArticles.single().copy(title = "Article B", url = "https://example.com/b")
        val collectionX = Collection("x", "Collection X")
        val collectionY = Collection("y", "Collection Y")
        var collectionClickCount = 0
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(
            restorationTester = restorationTester,
            articleDetailsRoute = { article, onBack, onCollectionsClick ->
                Text("Details ${article.title}")
                TextButton(onClick = onBack) { Text("Article details back") }
                TextButton(onClick = onCollectionsClick) { Text("Manage collections") }
            },
            collectionsRoute = { onBack, onCollectionClick ->
                Text("Collections")
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(
                    onClick = {
                        onCollectionClick(if (collectionClickCount++ == 0) collectionX else collectionY)
                    },
                ) { Text("Open collection") }
            },
            collectionDetailsRoute = { collection, onArticleClick, onBack ->
                Text("Details ${collection.name}")
                TextButton(onClick = onBack) { Text("Collection details back") }
                if (collection.id == collectionX.id) {
                    TextButton(onClick = { onArticleClick(articleB) }) { Text("Open Article B") }
                }
            },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.saved_open_collections)).performClick()
        composeTestRule.onNodeWithText("Open collection").performClick()
        composeTestRule.onNodeWithText("Open Article B").performClick()
        composeTestRule.onNodeWithText("Manage collections").performClick()
        composeTestRule.onNodeWithText("Open collection").performClick()
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.onNodeWithText("Details Collection Y").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collection details back").performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()
        composeTestRule.onNodeWithText("Collections back").performClick()
        composeTestRule.onNodeWithText("Details Article B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Article details back").performClick()
        composeTestRule.onNodeWithText("Details Collection X").assertIsDisplayed()
    }

    @Test
    fun bottomNavIsHiddenOnDetails() {
        setContent()

        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()
    }

    @Test
    fun detailsIsNotAThirdTopLevelDestination() {
        setContent()

        composeTestRule.onNodeWithText("Details").assertDoesNotExist()
        composeTestRule.onNodeWithText("Article").assertDoesNotExist()
    }

    @Test
    fun bookmarkClickOnHeadlinesCardDoesNotOpenDetails() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).performClick()

        composeTestRule.onNodeWithText("Back").assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
    }

    @Test
    fun bookmarkClickOnSavedCardDoesNotOpenDetails() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).performClick()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_remove)).performClick()

        composeTestRule.onNodeWithText("Back").assertDoesNotExist()
        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
    }

    @Test
    fun headlinesSearchButtonOpensSearch() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    @Test
    fun searchBackReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.search_back)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
    }

    @Test
    fun searchResultTapOpensDetails() {
        val searchArticle = fakeArticles.single()
        setContent(
            searchUiState =
                SearchUiState.Results(
                    query = "Test",
                    articles = fakeArticles,
                    savedUrls = emptySet(),
                ),
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(searchArticle.title.orEmpty()).performClick()

        composeTestRule.onNodeWithText(searchArticle.title.orEmpty()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun detailsBackReturnsToSearch() {
        val searchArticle = fakeArticles.single()
        setContent(
            searchUiState =
                SearchUiState.Results(
                    query = "Test",
                    articles = fakeArticles,
                    savedUrls = emptySet(),
                ),
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()
        composeTestRule.onNodeWithText(searchArticle.title.orEmpty()).performClick()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    @Test
    fun searchQuerySurvivesDetailsBackRoundTrip() {
        val searchArticle = fakeArticles.single()
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TestSignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    headlinesRoute = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
                        TopHeadlinesScreen(
                            uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onSearchClick = onSearchClick,
                            onSettingsClick = onSettingsClick,
                            bottomBar = bottomBar,
                        )
                    },
                    savedRoute = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefRoute = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchRoute = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState =
                                SearchUiState.Results(
                                    query = initialQuery,
                                    articles = fakeArticles,
                                    savedUrls = emptySet(),
                                ),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onOpenTopicMonitoring = {},
                            onBack = onBack,
                        )
                    },
                    articleDetailsRoute = { article, onBack, _ ->
                        TestDetailsContent(article = article, onBack = onBack)
                    },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(resourceString(Res.string.top_headlines_search))
            .performClick()

        composeTestRule
            .onNode(hasSetTextAction())
            .performTextInput("Test")

        composeTestRule
            .onNode(hasSetTextAction())
            .assertTextEquals("Test")

        composeTestRule
            .onNodeWithText(searchArticle.title.orEmpty())
            .performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule
            .onNodeWithText("Back")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText(resourceString(Res.string.search_title))
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasSetTextAction())
            .assertTextEquals("Test")
    }

    @Test
    fun bottomNavIsHiddenOnSearch() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_search)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()
    }

    @Test
    fun searchIsNotAThirdTopLevelDestination() {
        setContent()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertDoesNotExist()
    }

    @Test
    fun headlinesSettingsBackReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_settings)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.settings_title)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.settings_back)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertIsDisplayed()
    }

    @Test
    fun settingsSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        setContent(restorationTester = restorationTester)

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_settings)).performClick()
        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(resourceString(Res.string.settings_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.settings_back)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.top_headlines_title)).assertIsDisplayed()
    }

    @Test
    fun settingsShowsOfflineSection() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_settings)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.settings_offline_section)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_downloaded_headlines)).assertIsDisplayed()
    }

    @Test
    fun bottomNavIsHiddenOnSettings() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_settings)).performClick()

        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_headlines)).assertDoesNotExist()
        composeTestRule.onNodeWithText(resourceString(Res.string.bottom_nav_saved)).assertDoesNotExist()
    }
}

@Composable
private fun TestSignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    headlinesRoute: @Composable (@Composable () -> Unit, (Article) -> Unit, () -> Unit, () -> Unit) -> Unit,
    savedRoute: @Composable (@Composable () -> Unit, (Article) -> Unit, () -> Unit) -> Unit,
    searchRoute: @Composable (String, (String) -> Unit, (Article) -> Unit, () -> Unit, () -> Unit) -> Unit,
    articleDetailsRoute: @Composable (Article, () -> Unit, () -> Unit) -> Unit,
    dailyBriefRoute: @Composable (@Composable () -> Unit, (Article) -> Unit) -> Unit,
    collectionsRoute: @Composable (() -> Unit, (Collection) -> Unit) -> Unit = { _, _ -> },
    collectionDetailsRoute: @Composable (Collection, (Article) -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
    topicMonitoringRoute: @Composable ((MonitoredTopic) -> Unit, () -> Unit) -> Unit = { _, _ -> },
    topicMatchesRoute: @Composable (MonitoredTopic, (Article) -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
    settingsRoute: @Composable (() -> Unit) -> Unit = {},
    savedArticleCount: Int = 0,
) {
    SignalBriefApp(onboardingCompleted, onCompleteOnboarding) {
        SignalBriefNavHost(savedArticleCount) { navigation ->
            testSignalBriefEntries(
                navigation,
                headlinesRoute,
                savedRoute,
                searchRoute,
                articleDetailsRoute,
                dailyBriefRoute,
                collectionsRoute,
                collectionDetailsRoute,
                topicMonitoringRoute,
                topicMatchesRoute,
                settingsRoute,
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.testSignalBriefEntries(
    navigation: SignalBriefNavigationContext,
    headlines: @Composable (@Composable () -> Unit, (Article) -> Unit, () -> Unit, () -> Unit) -> Unit,
    saved: @Composable (@Composable () -> Unit, (Article) -> Unit, () -> Unit) -> Unit,
    search: @Composable (String, (String) -> Unit, (Article) -> Unit, () -> Unit, () -> Unit) -> Unit,
    details: @Composable (Article, () -> Unit, () -> Unit) -> Unit,
    brief: @Composable (@Composable () -> Unit, (Article) -> Unit) -> Unit,
    collections: @Composable (() -> Unit, (Collection) -> Unit) -> Unit,
    collectionDetails: @Composable (Collection, (Article) -> Unit, () -> Unit) -> Unit,
    monitoring: @Composable ((MonitoredTopic) -> Unit, () -> Unit) -> Unit,
    matches: @Composable (MonitoredTopic, (Article) -> Unit, () -> Unit) -> Unit,
    settings: @Composable (() -> Unit) -> Unit,
) {
    entry<SignalBriefRoute.Headlines> {
        headlines(
            navigation.bottomBarFor(SignalBriefRoute.Headlines),
            navigation.openArticle,
            navigation.openSearch,
            navigation.openSettings,
        )
    }
    entry<SignalBriefRoute.DailyBrief> { brief(navigation.bottomBarFor(SignalBriefRoute.DailyBrief), navigation.openArticle) }
    entry<SignalBriefRoute.Saved> {
        saved(
            navigation.bottomBarFor(SignalBriefRoute.Saved),
            navigation.openArticle,
            navigation.openCollections,
        )
    }
    entry<SignalBriefRoute.Search> {
        search(
            navigation.searchQuery(),
            navigation.onSearchQueryChange,
            navigation.openArticle,
            navigation.openTopicMonitoring,
            navigation.back,
        )
    }
    entry<SignalBriefRoute.Settings> { settings(navigation.back) }
    entry<SignalBriefRoute.TopicMonitoring> { monitoring(navigation.openTopicMatches, navigation.back) }
    entry<SignalBriefRoute.TopicMatches> { route ->
        navigation.topicFor(route.topicId)?.let { matches(it, navigation.openArticle, navigation.back) }
            ?: InvalidDestination(route.topicId, navigation.back)
    }
    entry<SignalBriefRoute.ArticleDetails> { route ->
        navigation.articleFor(route.articleUrl)?.let { details(it, navigation.back, navigation.openCollections) }
            ?: InvalidDestination(route.articleUrl, navigation.back)
    }
    entry<SignalBriefRoute.Collections> { collections(navigation.back, navigation.openCollection) }
    entry<SignalBriefRoute.CollectionDetails> { route ->
        navigation.collectionFor(route.collectionId)?.let { collectionDetails(it, navigation.openArticle, navigation.back) }
            ?: InvalidDestination(route.collectionId, navigation.back)
    }
}
