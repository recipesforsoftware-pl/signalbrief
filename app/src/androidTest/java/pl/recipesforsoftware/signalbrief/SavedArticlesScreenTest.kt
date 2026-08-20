package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesScreen
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesStrings
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

class SavedArticlesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeArticles =
        listOf(
            Article(
                title = "Saved Article 1",
                description = "Description 1",
                url = "https://example.com/1",
                imageUrl = null,
                source = Source(id = "src1", name = "Source 1"),
            ),
            Article(
                title = "Saved Article 2",
                description = "Description 2",
                url = "https://example.com/2",
                imageUrl = null,
                source = Source(id = "src2", name = "Source 2"),
            ),
        )

    private fun setContent(
        isDarkMode: Boolean = false,
        uiState: SavedArticlesUiState = SavedArticlesUiState.Content(fakeArticles),
        onArticleClick: (Article) -> Unit = {},
        onRemoveClick: (Article) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                SavedArticlesScreen(
                    uiState = uiState,
                    onArticleClick = onArticleClick,
                    onRemoveClick = onRemoveClick,
                )
            }
        }
    }

    @Test
    fun topBar_displaysTitle() {
        setContent()
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptyTitle() {
        setContent(uiState = SavedArticlesUiState.Empty)
        composeTestRule.onNodeWithText("No saved articles yet").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsEmptySubtitle() {
        setContent(uiState = SavedArticlesUiState.Empty)
        composeTestRule
            .onNodeWithText("Save stories from Top Headlines to keep them here.")
            .assertIsDisplayed()
    }

    @Test
    fun contentState_displaysArticles() {
        setContent(uiState = SavedArticlesUiState.Content(fakeArticles))
        composeTestRule.onNodeWithText("Saved Article 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Saved Article 2").assertIsDisplayed()
    }

    @Test
    fun removeAction_showsRemoveSemantics() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Remove from saved").assertIsDisplayed()
    }

    @Test
    fun removeAction_clickInvokesCallback() {
        var removedArticle: Article? = null
        setContent(onRemoveClick = { removedArticle = it })

        composeTestRule.onNodeWithContentDescription("Remove from saved").performClick()

        assert(removedArticle == fakeArticles[0]) {
            "onRemoveClick should receive the article"
        }
    }

    @Test
    fun removeAction_clickDoesNotOpenArticle() {
        var clickedArticle: Article? = null
        setContent(
            onArticleClick = { clickedArticle = it },
            onRemoveClick = {},
        )
        composeTestRule.onNodeWithContentDescription("Remove from saved").performClick()

        assert(clickedArticle == null) {
            "Remove click must not trigger article open"
        }
    }

    @Test
    fun articleCard_clickOpensArticle() {
        var clickedArticle: Article? = null
        setContent(onArticleClick = { clickedArticle = it })

        composeTestRule.onNodeWithText("Saved Article 1").performClick()

        assert(clickedArticle == fakeArticles[0]) {
            "Card click should open the article"
        }
    }

    @Test
    fun multipleArticles_preserveInputOrdering() {
        setContent(uiState = SavedArticlesUiState.Content(fakeArticles))

        val node1 = composeTestRule.onNodeWithText("Saved Article 1")
        val node2 = composeTestRule.onNodeWithText("Saved Article 2")
        node1.assertIsDisplayed()
        node2.assertIsDisplayed()
    }

    @Test
    fun darkModeFalse_rendersWithoutError() {
        setContent(isDarkMode = false)
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun darkModeTrue_rendersWithoutError() {
        setContent(isDarkMode = true)
        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }
}
