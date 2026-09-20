package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsStrings
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: SettingsUiState = SettingsUiState(),
        onBack: () -> Unit = {},
        onClearDownloadedHeadlines: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                SettingsScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onClearDownloadedHeadlines = onClearDownloadedHeadlines,
                )
            }
        }
    }

    @Test
    fun topBar_displaysSettingsTitle() {
        setContent()
        composeTestRule.onNodeWithText(SettingsStrings.TOP_BAR_TITLE).assertIsDisplayed()
    }

    @Test
    fun backButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription(SettingsStrings.BACK).assertIsDisplayed()
    }

    @Test
    fun backButton_invokesCallback() {
        var backClicked = false
        setContent(onBack = { backClicked = true })

        composeTestRule.onNodeWithContentDescription(SettingsStrings.BACK).performClick()

        assertTrue("onBack should have been called", backClicked)
    }

    @Test
    fun offlineSection_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithText(SettingsStrings.OFFLINE_SECTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(SettingsStrings.DOWNLOADED_HEADLINES).assertIsDisplayed()
    }

    @Test
    fun positiveCount_showsHeadlinesAvailableOffline() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 37))
        composeTestRule.onNodeWithText("37 headlines available offline").assertIsDisplayed()
    }

    @Test
    fun singleCount_usesSingularCopy() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText("1 headline available offline").assertIsDisplayed()
    }

    @Test
    fun emptyCache_showsNoDownloadedHeadlines() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 0))
        composeTestRule.onNodeWithText(SettingsStrings.NO_DOWNLOADED_HEADLINES).assertIsDisplayed()
    }

    @Test
    fun clearAction_isDisplayedInOfflineSection() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).assertIsDisplayed()
    }

    @Test
    fun clearAction_disabledForZeroCount() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 0))
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).assertIsNotEnabled()
    }

    @Test
    fun clearAction_enabledForPositiveCount() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).assertIsEnabled()
    }

    @Test
    fun clearActionClick_opensConfirmationDialog() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).performClick()
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_CONFIRMATION_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_CONFIRMATION_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun cancelInConfirmationDialog_doesNotClear() {
        var clearClicked = false
        setContent(
            uiState = SettingsUiState(downloadedHeadlineCount = 1),
            onClearDownloadedHeadlines = { clearClicked = true },
        )

        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).performClick()
        composeTestRule.onNodeWithText(SettingsStrings.CANCEL).performClick()

        assertTrue("clear callback should not fire on cancel", !clearClicked)
    }

    @Test
    fun confirmInConfirmationDialog_clears() {
        var clearClicked = false
        setContent(
            uiState = SettingsUiState(downloadedHeadlineCount = 1),
            onClearDownloadedHeadlines = { clearClicked = true },
        )

        composeTestRule.onNodeWithText(SettingsStrings.CLEAR_DOWNLOADED_HEADLINES).performClick()
        composeTestRule.onNodeWithText(SettingsStrings.CLEAR).performClick()

        assertTrue("clear callback should fire on confirm", clearClicked)
    }
}
