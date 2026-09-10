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
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefStrings
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefUiState
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesStrings
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesUiState
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchStrings
import pl.recipesforsoftware.signalbrief.ui.search.SearchUiState
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsStrings
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesUiState
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesStrings
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesUiState
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringStrings
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiState

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
        articleDetailsContent: @Composable (article: Article, onBack: () -> Unit, onCollectionsClick: () -> Unit) -> Unit =
            { article, onBack, _ -> TestDetailsContent(article, onBack) },
        collectionsContent: @Composable (onBack: () -> Unit, onCollectionClick: (Collection) -> Unit) -> Unit =
            { onBack, _ ->
                TextButton(onClick = onBack) { Text("Collections back") }
                Text("Collections")
            },
        collectionDetailsContent: @Composable (collection: Collection, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit =
            { collection, _, onBack ->
                TextButton(onClick = onBack) { Text("Collection details back") }
                Text("Collection details ${collection.id}: ${collection.name}")
            },
        topicMonitoringContent: @Composable (onOpenTopicMatches: (MonitoredTopic) -> Unit, onBack: () -> Unit) -> Unit =
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
        topicMatchesContent: @Composable (topic: MonitoredTopic, onArticleClick: (Article) -> Unit, onBack: () -> Unit) -> Unit =
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
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
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
                    savedContent = { bottomBar, onArticleClick, onCollectionsClick ->
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
                    dailyBriefContent = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(headlineArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, monitoring, onBack ->
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
                    articleDetailsContent = articleDetailsContent,
                    collectionsContent = collectionsContent,
                    collectionDetailsContent = collectionDetailsContent,
                    topicMonitoringContent = topicMonitoringContent,
                    topicMatchesContent = topicMatchesContent,
                    settingsContent = { onBack ->
                        SettingsScreen(
                            uiState = SettingsUiState(downloadedHeadlineCount = 0),
                            onBack = onBack,
                        )
                    },
                )
            }
        }
    }

    @Test
    fun searchTopicMonitoringBackPreservesQuery() {
        setContent()
        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Kotlin")
        composeTestRule.onNodeWithText(SearchStrings.MONITORED_TOPICS).performClick()
        composeTestRule.onNodeWithText(TopicMonitoringStrings.TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(TopicMonitoringStrings.BACK).performClick()
        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertTextEquals("Kotlin")
    }

    @Test
    fun searchTopicMatchesRoundTripPreservesQuery() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Android")
        composeTestRule.onNodeWithText(SearchStrings.MONITORED_TOPICS).performClick()
        composeTestRule.onNodeWithText(TopicMonitoringStrings.TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithText("Kotlin").performClick()
        composeTestRule.onNodeWithText(TopicMatchesStrings.NO_MATCHES_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(TopicMatchesStrings.BACK).performClick()
        composeTestRule.onNodeWithText(TopicMonitoringStrings.TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(TopicMonitoringStrings.BACK).performClick()
        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).assertTextEquals("Android")
    }

    @Test
    fun topicMatchesArticleDetailsBackReturnsToTopicMatches() {
        setContent(
            topicMatchesContent = { topic, onArticleClick, onBack ->
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

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
        composeTestRule.onNodeWithText(SearchStrings.MONITORED_TOPICS).performClick()
        composeTestRule.onNodeWithText("Kotlin").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText(TopicMatchesStrings.NO_MATCHES_TITLE).assertDoesNotExist()
    }

    @Test
    fun headlinesIsDefaultDestination() {
        setContent()

        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun tappingSavedShowsSavedDestination() {
        setContent()

        composeTestRule.onNodeWithText("Saved").performClick()

        composeTestRule.onNodeWithText(SavedArticlesStrings.EMPTY_TITLE).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(SavedArticlesStrings.EMPTY_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun tappingHeadlinesReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.EMPTY_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithText("Headlines").performClick()
        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun exactlyThreeRealTopLevelDestinationsExist() {
        setContent()

        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
        composeTestRule.onNodeWithText("Brief").assertIsDisplayed()

        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
        composeTestRule.onNodeWithText("Monitor").assertDoesNotExist()
        composeTestRule.onNodeWithText("Collections").assertDoesNotExist()
    }

    @Test
    fun dailyBriefOpensAndDetailsBackReturnsToBrief() {
        setContent()

        composeTestRule.onNodeWithText("Brief").performClick()
        composeTestRule.onNodeWithText(DailyBriefStrings.INTRO).assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText(DailyBriefStrings.INTRO).assertIsDisplayed()
    }

    @Test
    fun savedDestinationSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
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
                    savedContent = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefContent = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
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
                    articleDetailsContent = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.EMPTY_TITLE).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(SavedArticlesStrings.EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(SavedArticlesStrings.EMPTY_TITLE).assertIsDisplayed()
    }

    @Test
    fun dailyBriefDestinationSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
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
                    savedContent = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefContent = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
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
                    articleDetailsContent = { _, _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("Brief").performClick()
        composeTestRule.onNodeWithText(DailyBriefStrings.INTRO).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(DailyBriefStrings.INTRO).assertIsDisplayed()
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

        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
    }

    @Test
    fun savedArticleTapOpensDetails() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun detailsBackReturnsToSaved() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
    }

    @Test
    fun detailsCollectionsBackReturnsToSameArticle() {
        setContent(
            articleDetailsContent = { article, onBack, onCollectionsClick ->
                TextButton(onClick = onBack) { Text("Back") }
                Text(article.title.orEmpty())
                TextButton(onClick = onCollectionsClick) { Text("Manage collections") }
            },
        )

        composeTestRule.onNodeWithText("Test Article").performClick()
        composeTestRule.onNodeWithText("Manage collections").performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collections back").performClick()

        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage collections").assertIsDisplayed()
    }

    @Test
    fun savedCollectionsBackReturnsToSaved() {
        setContent()

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.OPEN_COLLECTIONS).performClick()
        composeTestRule.onNodeWithText("Collections").assertIsDisplayed()

        composeTestRule.onNodeWithText("Collections back").performClick()

        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun collectionsDetailsBackReturnsToCollections() {
        val collection = Collection("42", "Reading")
        setContent(
            collectionsContent = { onBack, onCollectionClick ->
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
        )

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.OPEN_COLLECTIONS).performClick()
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
            articleDetailsContent = { article, onBack, _ ->
                TextButton(onClick = onBack) { Text("Article details back") }
                Text("Article details: ${article.url}")
            },
            collectionsContent = { onBack, onCollectionClick ->
                TextButton(onClick = onBack) { Text("Collections back") }
                TextButton(onClick = { onCollectionClick(collection) }) { Text("Open Reading") }
                Text("Collections")
            },
            collectionDetailsContent = { selected, onArticleClick, onBack ->
                TextButton(onClick = onBack) { Text("Collection details back") }
                Text("Collection details ${selected.id}: ${selected.name}")
                TextButton(onClick = { onArticleClick(fakeArticles.single()) }) { Text("Open member article") }
            },
        )

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.OPEN_COLLECTIONS).performClick()
        composeTestRule.onNodeWithText("Open Reading").performClick()
        composeTestRule.onNodeWithText("Open member article").performClick()
        composeTestRule.onNodeWithText("Article details: https://example.com/1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Article details back").performClick()

        composeTestRule.onNodeWithText("Collection details 42: Reading").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open member article").assertIsDisplayed()
    }

    @Test
    fun bottomNavIsHiddenOnDetails() {
        setContent()

        composeTestRule.onNodeWithText("Test Article").performClick()

        composeTestRule.onNodeWithText("Headlines").assertDoesNotExist()
        composeTestRule.onNodeWithText("Saved").assertDoesNotExist()
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

        composeTestRule.onNodeWithContentDescription("Save article").performClick()

        composeTestRule.onNodeWithText("Back").assertDoesNotExist()
        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun bookmarkClickOnSavedCardDoesNotOpenDetails() {
        setContent(savedArticles = fakeArticles)

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithContentDescription("Remove from saved").performClick()

        composeTestRule.onNodeWithText("Back").assertDoesNotExist()
        composeTestRule.onNodeWithText("Test Article").assertIsDisplayed()
    }

    @Test
    fun headlinesSearchButtonOpensSearch() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()

        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun searchBackReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(SearchStrings.BACK).performClick()

        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
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

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
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

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()
        composeTestRule.onNodeWithText(searchArticle.title.orEmpty()).performClick()
        composeTestRule.onNodeWithText("Back").assertIsDisplayed()

        composeTestRule.onNodeWithText("Back").performClick()

        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun searchQuerySurvivesDetailsBackRoundTrip() {
        val searchArticle = fakeArticles.single()

        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick, onSettingsClick ->
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
                    savedContent = { bottomBar, onArticleClick, _ ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    dailyBriefContent = { bottomBar, onArticleClick ->
                        DailyBriefScreen(
                            uiState = DailyBriefUiState.Content(fakeArticles, emptySet()),
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, _, onBack ->
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
                    articleDetailsContent = { article, onBack, _ ->
                        TestDetailsContent(article = article, onBack = onBack)
                    },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(TopHeadlinesStrings.SEARCH)
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

        composeTestRule
            .onNodeWithText("Back")
            .assertIsDisplayed()
            .performClick()

        composeTestRule
            .onNodeWithText(SearchStrings.TOP_BAR_TITLE)
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasSetTextAction())
            .assertTextEquals("Test")
    }

    @Test
    fun bottomNavIsHiddenOnSearch() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SEARCH).performClick()

        composeTestRule.onNodeWithText("Headlines").assertDoesNotExist()
        composeTestRule.onNodeWithText("Saved").assertDoesNotExist()
    }

    @Test
    fun searchIsNotAThirdTopLevelDestination() {
        setContent()

        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
        composeTestRule.onNodeWithText(SearchStrings.TOP_BAR_TITLE).assertDoesNotExist()
    }

    @Test
    fun headlinesSettingsBackReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SETTINGS).performClick()

        composeTestRule.onNodeWithText(SettingsStrings.TOP_BAR_TITLE).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(SettingsStrings.BACK).performClick()

        composeTestRule.onNodeWithText(TopHeadlinesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
    }

    @Test
    fun settingsShowsOfflineSection() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SETTINGS).performClick()

        composeTestRule.onNodeWithText(SettingsStrings.OFFLINE_SECTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(SettingsStrings.DOWNLOADED_HEADLINES).assertIsDisplayed()
    }

    @Test
    fun bottomNavIsHiddenOnSettings() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.SETTINGS).performClick()

        composeTestRule.onNodeWithText("Headlines").assertDoesNotExist()
        composeTestRule.onNodeWithText("Saved").assertDoesNotExist()
    }
}
