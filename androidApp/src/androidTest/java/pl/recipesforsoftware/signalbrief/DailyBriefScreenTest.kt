package pl.recipesforsoftware.signalbrief

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.daily_brief_empty_subtitle
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.daily_brief_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.daily_brief_intro
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.daily_brief_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_remove
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_save
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefScreen
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

private fun resourceString(resource: StringResource): String = runBlocking { getString(resource) }

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

        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_intro)).assertIsDisplayed()
    }

    @Test
    fun empty_displaysTitleAndSubtitle() {
        setContent(uiState = DailyBriefUiState.Empty)

        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_empty_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.daily_brief_empty_subtitle)).assertIsDisplayed()
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

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).performClick()

        assert(bookmarkedArticle == article) { "onBookmarkClick should receive the article" }
    }

    @Test
    fun bookmarkClick_doesNotInvokeArticleCallback() {
        var clickedArticle: Article? = null
        setContent(onArticleClick = { clickedArticle = it })

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).performClick()

        assert(clickedArticle == null) { "Bookmark click must not trigger article open" }
    }

    @Test
    fun savedArticle_exposesRemoveSemantics() {
        setContent(uiState = DailyBriefUiState.Content(listOf(article), setOf(article.url)))

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_remove)).assertIsDisplayed()
    }

    @Test
    fun unsavedArticle_exposesSaveSemantics() {
        setContent()

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).assertIsDisplayed()
    }

    @Test
    fun suppliedBottomBar_renders() {
        setContent(bottomBar = { Text("Daily Brief test bottom bar") })

        composeTestRule.onNodeWithText("Daily Brief test bottom bar").assertIsDisplayed()
    }
}
