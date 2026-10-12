package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_idle_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_local_articles_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_results_subtitle
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_results_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_remove
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.top_headlines_bookmark_save
import pl.recipesforsoftware.signalbrief.ui.search.SearchScreen
import pl.recipesforsoftware.signalbrief.ui.search.SearchUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

private fun resourceString(
    resource: StringResource,
    vararg args: Any,
): String = runBlocking { getString(resource, *args) }

class SearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeArticles =
        listOf(
            Article(
                title = "Kotlin Multiplatform",
                description = "A great Kotlin story",
                url = "https://example.com/1",
                imageUrl = null,
                source = Source(id = "src1", name = "Kotlin Weekly"),
            ),
            Article(
                title = "Android Studio",
                description = "Android tooling updates",
                url = "https://example.com/2",
                imageUrl = null,
                source = Source(id = "src2", name = "Android News"),
            ),
        )

    private fun setContent(
        query: String = "",
        uiState: SearchUiState = SearchUiState.Idle,
        onQueryChange: (String) -> Unit = {},
        onArticleClick: (Article) -> Unit = {},
        onBookmarkClick: ((Article) -> Unit)? = null,
        onOpenTopicMonitoring: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SearchScreen(
                    query = query,
                    onQueryChange = onQueryChange,
                    uiState = uiState,
                    onArticleClick = onArticleClick,
                    onBookmarkClick = onBookmarkClick,
                    onOpenTopicMonitoring = onOpenTopicMonitoring,
                    onBack = onBack,
                )
            }
        }
    }

    @Test
    fun topBar_displaysTitle() {
        setContent()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    @Test
    fun backButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.search_back)).assertIsDisplayed()
    }

    @Test
    fun backButton_invokesCallback() {
        var backClicked = false
        setContent(onBack = { backClicked = true })

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.search_back)).performClick()

        assert(backClicked) { "onBack should have been called" }
    }

    @Test
    fun searchField_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_headlines)).assertIsDisplayed()
    }

    @Test
    fun queryCallback_firesOnInput() {
        var receivedQuery: String? = null
        setContent(onQueryChange = { receivedQuery = it })

        composeTestRule.onNodeWithText(resourceString(Res.string.search_headlines)).performTextInput("kotlin")

        assertEquals("kotlin", receivedQuery)
    }

    @Test
    fun idleState_showsIdleTitle() {
        setContent(uiState = SearchUiState.Idle)
        composeTestRule.onNodeWithText(resourceString(Res.string.search_idle_title)).assertIsDisplayed()
    }

    @Test
    fun noLocalArticlesState_showsTitle() {
        setContent(uiState = SearchUiState.NoLocalArticles)
        composeTestRule.onNodeWithText(resourceString(Res.string.search_no_local_articles_title)).assertIsDisplayed()
    }

    @Test
    fun noResultsState_showsTitle() {
        setContent(uiState = SearchUiState.NoResults("swift"))
        composeTestRule.onNodeWithText(resourceString(Res.string.search_no_results_title)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(resourceString(Res.string.search_no_results_subtitle, "swift"))
            .assertIsDisplayed()
    }

    @Test
    fun resultsState_displaysArticles() {
        setContent(
            query = "kotlin",
            uiState = SearchUiState.Results("kotlin", fakeArticles, savedUrls = emptySet()),
        )

        composeTestRule.onNodeWithText("Kotlin Multiplatform").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android Studio").assertIsDisplayed()
    }

    @Test
    fun resultBodyClick_invokesArticleClick() {
        var clickedArticle: Article? = null
        setContent(
            query = "kotlin",
            uiState = SearchUiState.Results("kotlin", fakeArticles, savedUrls = emptySet()),
            onArticleClick = { clickedArticle = it },
        )

        composeTestRule.onNodeWithText("Kotlin Multiplatform").performClick()

        assert(clickedArticle == fakeArticles[0]) {
            "onArticleClick should receive the clicked article"
        }
    }

    @Test
    fun bookmarkClick_invokesCallback() {
        var bookmarkedArticle: Article? = null
        setContent(
            uiState = SearchUiState.Results("kotlin", fakeArticles, savedUrls = emptySet()),
            onBookmarkClick = { bookmarkedArticle = it },
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).performClick()

        assert(bookmarkedArticle == fakeArticles[0]) {
            "onBookmarkClick should receive the article"
        }
    }

    @Test
    fun bookmarkClick_doesNotOpenArticle() {
        var clickedArticle: Article? = null
        setContent(
            uiState = SearchUiState.Results("kotlin", fakeArticles, savedUrls = emptySet()),
            onArticleClick = { clickedArticle = it },
            onBookmarkClick = {},
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_save)).performClick()

        assert(clickedArticle == null) {
            "Bookmark click must not trigger article open"
        }
    }

    @Test
    fun savedBookmark_showsRemoveSemantics() {
        setContent(
            uiState =
                SearchUiState.Results(
                    "kotlin",
                    fakeArticles,
                    savedUrls = setOf("https://example.com/1"),
                ),
            onBookmarkClick = {},
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.top_headlines_bookmark_remove)).assertIsDisplayed()
    }

    @Test
    fun darkModeFalse_rendersWithoutError() {
        setContent()
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    @Test
    fun darkModeTrue_rendersWithoutError() {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = true, dynamicColor = false) {
                SearchScreen(
                    query = "",
                    onQueryChange = {},
                    uiState = SearchUiState.Idle,
                    onArticleClick = {},
                    onBookmarkClick = {},
                    onOpenTopicMonitoring = {},
                    onBack = {},
                )
            }
        }
        composeTestRule.onNodeWithText(resourceString(Res.string.search_title)).assertIsDisplayed()
    }

    private fun assertEquals(
        expected: String,
        actual: String?,
    ) {
        assert(actual == expected) { "Expected '$expected' but was '$actual'" }
    }
}
