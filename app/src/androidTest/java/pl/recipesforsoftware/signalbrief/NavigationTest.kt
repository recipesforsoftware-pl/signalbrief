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
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesStrings
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesUiState
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchStrings
import pl.recipesforsoftware.signalbrief.ui.search.SearchUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesUiState

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
        articleDetailsContent: @Composable (article: Article, onBack: () -> Unit) -> Unit = ::TestDetailsContent,
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
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
                            bottomBar = bottomBar,
                        )
                    },
                    savedContent = { bottomBar, onArticleClick ->
                        SavedArticlesScreen(
                            uiState =
                                if (savedArticles.isEmpty()) {
                                    SavedArticlesUiState.Empty
                                } else {
                                    SavedArticlesUiState.Content(savedArticles)
                                },
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState = searchUiState,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onBack = onBack,
                        )
                    },
                    articleDetailsContent = articleDetailsContent,
                )
            }
        }
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
    fun onlyHeadlinesAndSavedDestinationsExist() {
        setContent()

        composeTestRule.onNodeWithText("Headlines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()

        composeTestRule.onNodeWithText("Search").assertDoesNotExist()
        composeTestRule.onNodeWithText("Daily Brief").assertDoesNotExist()
        composeTestRule.onNodeWithText("Monitor").assertDoesNotExist()
        composeTestRule.onNodeWithText("Collections").assertDoesNotExist()
    }

    @Test
    fun savedDestinationSurvivesStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
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
                            bottomBar = bottomBar,
                        )
                    },
                    savedContent = { bottomBar, onArticleClick ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, onBack ->
                        SearchScreen(
                            query = initialQuery,
                            onQueryChange = onQueryChange,
                            uiState = SearchUiState.Idle,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onBack = onBack,
                        )
                    },
                    articleDetailsContent = { _, _ -> },
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
                    topHeadlinesContent = { bottomBar, onArticleClick, onSearchClick ->
                        TopHeadlinesScreen(
                            uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
                            onBookmarkClick = {},
                            onSearchClick = onSearchClick,
                            bottomBar = bottomBar,
                        )
                    },
                    savedContent = { bottomBar, onArticleClick ->
                        SavedArticlesScreen(
                            uiState = SavedArticlesUiState.Empty,
                            onArticleClick = onArticleClick,
                            onRemoveClick = {},
                            bottomBar = bottomBar,
                        )
                    },
                    searchContent = { initialQuery, onQueryChange, onArticleClick, onBack ->
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
                            onBack = onBack,
                        )
                    },
                    articleDetailsContent = { article, onBack ->
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
}
