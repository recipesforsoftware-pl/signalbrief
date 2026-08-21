package pl.recipesforsoftware.signalbrief

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesStrings
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesUiState
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
        articleDetailsContent: @Composable (article: Article, onBack: () -> Unit) -> Unit = ::TestDetailsContent,
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                SignalBriefApp(
                    onboardingCompleted = true,
                    onCompleteOnboarding = {},
                    topHeadlinesContent = { bottomBar, onArticleClick ->
                        TopHeadlinesScreen(
                            uiState =
                                TopHeadlinesUiState.Success(
                                    headlineArticles,
                                    FeedSource.NETWORK,
                                ),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
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

        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(SavedArticlesStrings.EMPTY_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun tappingHeadlinesReturnsToHeadlines() {
        setContent()

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()

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
                    topHeadlinesContent = { bottomBar, onArticleClick ->
                        TopHeadlinesScreen(
                            uiState =
                                TopHeadlinesUiState.Success(
                                    fakeArticles,
                                    FeedSource.NETWORK,
                                ),
                            onRefresh = {},
                            onArticleClick = onArticleClick,
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
                    articleDetailsContent = { _, _ -> },
                )
            }
        }

        composeTestRule.onNodeWithText("Saved").performClick()
        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()
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

        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
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
        composeTestRule.onNodeWithText(SavedArticlesStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }
}
