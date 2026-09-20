package pl.recipesforsoftware.signalbrief

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefStrings
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings

class DailyBriefScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val article =
        Article(
            title = "Daily Brief Article",
            description = "A concise briefing",
            url = "https://example.com/daily-brief",
            imageUrl = null,
            source = Source(id = "daily", name = "Daily Source"),
        )

    private fun setContent(
        uiState: DailyBriefUiState = DailyBriefUiState.Content(listOf(article), emptySet()),
        onArticleClick: (Article) -> Unit = {},
        onBookmarkClick: (Article) -> Unit = {},
        bottomBar: @Composable () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                DailyBriefScreen(
                    uiState = uiState,
                    onArticleClick = onArticleClick,
                    onBookmarkClick = onBookmarkClick,
                    bottomBar = bottomBar,
                )
            }
        }
    }

    @Test
    fun content_displaysTitleAndIntro() {
        setContent()

        composeTestRule.onNodeWithText(DailyBriefStrings.TOP_BAR_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(DailyBriefStrings.INTRO).assertIsDisplayed()
    }

    @Test
    fun empty_displaysTitleAndSubtitle() {
        setContent(uiState = DailyBriefUiState.Empty)

        composeTestRule.onNodeWithText(DailyBriefStrings.EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(DailyBriefStrings.EMPTY_SUBTITLE).assertIsDisplayed()
    }

    @Test
    fun content_displaysArticle() {
        setContent()

        composeTestRule.onNodeWithText(article.title.orEmpty()).assertIsDisplayed()
    }

    @Test
    fun articleBodyClick_invokesArticleCallback() {
        var clickedArticle: Article? = null
        setContent(onArticleClick = { clickedArticle = it })

        composeTestRule.onNodeWithText(article.title.orEmpty()).performClick()

        assert(clickedArticle == article) { "onArticleClick should receive the article" }
    }

    @Test
    fun bookmarkClick_invokesBookmarkCallback() {
        var bookmarkedArticle: Article? = null
        setContent(onBookmarkClick = { bookmarkedArticle = it })

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.BOOKMARK_SAVE).performClick()

        assert(bookmarkedArticle == article) { "onBookmarkClick should receive the article" }
    }

    @Test
    fun bookmarkClick_doesNotInvokeArticleCallback() {
        var clickedArticle: Article? = null
        setContent(onArticleClick = { clickedArticle = it })

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.BOOKMARK_SAVE).performClick()

        assert(clickedArticle == null) { "Bookmark click must not trigger article open" }
    }

    @Test
    fun savedArticle_exposesRemoveSemantics() {
        setContent(uiState = DailyBriefUiState.Content(listOf(article), setOf(article.url)))

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.BOOKMARK_REMOVE).assertIsDisplayed()
    }

    @Test
    fun unsavedArticle_exposesSaveSemantics() {
        setContent()

        composeTestRule.onNodeWithContentDescription(TopHeadlinesStrings.BOOKMARK_SAVE).assertIsDisplayed()
    }

    @Test
    fun suppliedBottomBar_renders() {
        setContent(bottomBar = { Text("Daily Brief test bottom bar") })

        composeTestRule.onNodeWithText("Daily Brief test bottom bar").assertIsDisplayed()
    }
}
