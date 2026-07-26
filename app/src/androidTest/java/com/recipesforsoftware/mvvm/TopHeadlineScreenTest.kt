package com.recipesforsoftware.mvvm

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.recipesforsoftware.mvvm.data.model.Article
import com.recipesforsoftware.mvvm.data.model.Source
import com.recipesforsoftware.mvvm.ui.base.UiState
import com.recipesforsoftware.mvvm.ui.screens.TopHeadlineScreen
import com.recipesforsoftware.mvvm.ui.theme.NewsAppTheme
import org.junit.Rule
import org.junit.Test

class TopHeadlineScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeArticles = listOf(
        Article(
            title = "Test Article 1",
            description = "Description 1",
            url = "https://example.com/1",
            imageUrl = null,
            source = Source(id = "src1", name = "Source 1")
        ),
        Article(
            title = "Test Article 2",
            description = "Description 2",
            url = "https://example.com/2",
            imageUrl = null,
            source = Source(id = "src2", name = "Source 2")
        )
    )

    private fun setContent(
        isDarkMode: Boolean = false,
        uiState: UiState<List<Article>> = UiState.Success(fakeArticles),
        onRefresh: () -> Unit = {},
        onToggleDarkMode: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            NewsAppTheme(isDarkMode = isDarkMode, dynamicColor = false) {
                TopHeadlineScreen(
                    uiState = uiState,
                    isDarkMode = isDarkMode,
                    onRefresh = onRefresh,
                    onToggleDarkMode = onToggleDarkMode
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
    fun loadingState_showsLoadingIndicator() {
        setContent(uiState = UiState.Loading)
        composeTestRule.onNodeWithText("Loading headlines...").assertIsDisplayed()
    }

    @Test
    fun errorState_showsErrorMessage() {
        setContent(uiState = UiState.Error("Network connection failed"))
        composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
    }

    @Test
    fun errorState_showsRetryButton() {
        setContent(uiState = UiState.Error("Network error"))
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    }

    @Test
    fun errorState_retryCallsOnRefresh() {
        var retried = false
        setContent(
            uiState = UiState.Error("Network error"),
            onRefresh = { retried = true }
        )

        composeTestRule.onNodeWithText("Try Again").performClick()

        assert(retried) { "onRefresh should have been called on retry" }
    }

    @Test
    fun successState_displaysArticles() {
        setContent(uiState = UiState.Success(fakeArticles))
        composeTestRule.onNodeWithText("Test Article 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Article 2").assertIsDisplayed()
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
}
