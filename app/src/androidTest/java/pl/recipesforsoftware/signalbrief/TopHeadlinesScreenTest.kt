package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DarkModeMenu
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesError
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesScreen
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesUiState

class TopHeadlinesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeArticles =
        listOf(
            Article(
                title = "Test Article 1",
                description = "Description 1",
                url = "https://example.com/1",
                imageUrl = null,
                source = Source(id = "src1", name = "Source 1"),
            ),
            Article(
                title = "Test Article 2",
                description = "Description 2",
                url = "https://example.com/2",
                imageUrl = null,
                source = Source(id = "src2", name = "Source 2"),
            ),
        )

    private fun setContent(
        isDarkMode: Boolean = false,
        uiState: TopHeadlinesUiState =
            TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
        onRefresh: () -> Unit = {},
        onArticleClick: (Article) -> Unit = {},
        onBookmarkClick: ((Article) -> Unit)? = null,
        onToggleDarkMode: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                TopHeadlinesScreen(
                    uiState = uiState,
                    onRefresh = onRefresh,
                    onArticleClick = onArticleClick,
                    onBookmarkClick = onBookmarkClick,
                    topBarActions = {
                        DarkModeMenu(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode,
                        )
                    },
                )
            }
        }
    }

    // --- Top Bar ---

    @Test
    fun topBar_displaysTitle() {
        setContent()
        composeTestRule.onNodeWithText("Top Headlines").assertIsDisplayed()
    }

    @Test
    fun topBar_displaysSubtitle() {
        setContent()
        composeTestRule.onNodeWithText("Latest news from around the world").assertIsDisplayed()
    }

    // --- Menu button ---

    @Test
    fun menuButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
    }

    @Test
    fun menuButton_opensDropdownMenu() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Dark Mode").assertIsDisplayed()
    }

    // --- Dark Mode Toggle ---

    @Test
    fun darkModeToggle_togglesWhenClicked() {
        var toggled = false
        setContent(onToggleDarkMode = { toggled = true })

        // Open menu and click toggle
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Dark Mode").performClick()

        assert(toggled) { "onToggleDarkMode should have been called" }
    }

    @Test
    fun darkModeToggle_menuClosesAfterToggle() {
        setContent(onToggleDarkMode = {})

        // Open menu
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Dark Mode").assertIsDisplayed()

        // Click toggle
        composeTestRule.onNodeWithText("Dark Mode").performClick()

        // Menu should be dismissed - "Dark Mode" should no longer be visible
        composeTestRule.onNodeWithText("Dark Mode").assertDoesNotExist()
    }

    // --- Refresh button ---

    @Test
    fun refreshButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Refresh").assertIsDisplayed()
    }

    @Test
    fun refreshButton_callsOnRefresh() {
        var refreshed = false
        setContent(onRefresh = { refreshed = true })

        composeTestRule.onNodeWithContentDescription("Refresh").performClick()

        assert(refreshed) { "onRefresh should have been called" }
    }

    // --- Content states ---

    @Test
    fun loadingState_showsLoadingDescription() {
        setContent(uiState = TopHeadlinesUiState.Loading)
        composeTestRule.onNodeWithContentDescription("Loading headlines...").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        setContent(uiState = TopHeadlinesUiState.Error(TopHeadlinesError.Network))
        composeTestRule
            .onNodeWithText(TopHeadlinesStrings.errorBody(TopHeadlinesError.Network))
            .assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() {
        setContent(uiState = TopHeadlinesUiState.Error(TopHeadlinesError.Network))
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    }

    @Test
    fun errorState_retryCallsOnRefresh() {
        var retried = false
        setContent(
            uiState = TopHeadlinesUiState.Error(TopHeadlinesError.Network),
            onRefresh = { retried = true },
        )

        composeTestRule.onNodeWithText("Try Again").performClick()

        assert(retried) { "onRefresh should have been called on retry" }
    }

    @Test
    fun successState_displaysArticles() {
        setContent(uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK))
        composeTestRule.onNodeWithText("Test Article 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article 2").assertIsDisplayed()
    }

    @Test
    fun cachedSuccess_showsCacheNoticeBanner() {
        setContent(uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.CACHE))
        composeTestRule.onNodeWithText("Showing saved headlines").assertIsDisplayed()
    }

    @Test
    fun networkSuccess_doesNotShowCacheNoticeBanner() {
        setContent(uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK))
        composeTestRule.onNodeWithText("Showing saved headlines").assertDoesNotExist()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        setContent(uiState = TopHeadlinesUiState.Empty)
        composeTestRule.onNodeWithText("No headlines available").assertIsDisplayed()
    }

    // --- Article click ---

    @Test
    fun articleCard_clickInvokesOnArticleClickWithArticle() {
        var clickedArticle: Article? = null
        setContent(
            uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
            onArticleClick = { clickedArticle = it },
        )

        composeTestRule.onNodeWithText("Test Article 1").performClick()

        assert(clickedArticle == fakeArticles[0]) {
            "onArticleClick should receive the clicked article"
        }
    }

    @Test
    fun articleCard_clickOnDescriptionInvokesOnArticleClick() {
        var clickedArticle: Article? = null
        setContent(
            uiState = TopHeadlinesUiState.Success(fakeArticles, FeedSource.NETWORK),
            onArticleClick = { clickedArticle = it },
        )

        composeTestRule.onNodeWithText("Description 2").performClick()

        assert(clickedArticle == fakeArticles[1]) {
            "onArticleClick should receive the clicked article when tapping the description"
        }
    }

    // --- Dark mode light/dark state rendering ---

    @Test
    fun darkModeFalse_rendersWithoutError() {
        setContent(isDarkMode = false)
        composeTestRule.onNodeWithText("Top Headlines").assertIsDisplayed()
    }

    @Test
    fun darkModeTrue_rendersWithoutError() {
        setContent(isDarkMode = true)
        composeTestRule.onNodeWithText("Top Headlines").assertIsDisplayed()
    }

    // --- Bookmark action ---

    @Test
    fun bookmarkAction_unsaved_showsSaveSemantics() {
        setContent(
            onBookmarkClick = {},
        )
        composeTestRule.onNodeWithContentDescription("Save article").assertIsDisplayed()
    }

    @Test
    fun bookmarkAction_saved_showsRemoveSemantics() {
        setContent(
            uiState =
                TopHeadlinesUiState.Success(
                    fakeArticles,
                    FeedSource.NETWORK,
                    savedUrls = setOf("https://example.com/1"),
                ),
            onBookmarkClick = {},
        )
        composeTestRule.onNodeWithContentDescription("Remove from saved").assertIsDisplayed()
    }

    @Test
    fun bookmarkAction_clickInvokesCallback() {
        var bookmarkedArticle: Article? = null
        setContent(
            onBookmarkClick = { bookmarkedArticle = it },
        )
        composeTestRule.onNodeWithContentDescription("Save article").performClick()

        assert(bookmarkedArticle == fakeArticles[0]) {
            "onBookmarkClick should receive the article"
        }
    }

    @Test
    fun bookmarkAction_clickDoesNotOpenArticle() {
        var clickedArticle: Article? = null
        setContent(
            onArticleClick = { clickedArticle = it },
            onBookmarkClick = {},
        )
        composeTestRule.onNodeWithContentDescription("Save article").performClick()

        assert(clickedArticle == null) {
            "Bookmark click must not trigger article open"
        }
    }

    @Test
    fun articleCard_clickStillOpensActionableArticle() {
        var clickedArticle: Article? = null
        setContent(
            onArticleClick = { clickedArticle = it },
            onBookmarkClick = {},
        )
        composeTestRule.onNodeWithText("Test Article 1").performClick()

        assert(clickedArticle == fakeArticles[0]) {
            "Card click should still open the article"
        }
    }
}
