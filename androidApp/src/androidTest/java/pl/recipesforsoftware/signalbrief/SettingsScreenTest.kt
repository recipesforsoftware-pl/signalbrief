package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_cancel
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_clear
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_clear_confirmation_message
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_clear_confirmation_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_clear_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_downloaded_headlines_count
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_no_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_offline_section
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.settings_title
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsScreen
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

private fun resourceString(resource: StringResource): String = runBlocking { getString(resource) }

private fun pluralString(
    resource: PluralStringResource,
    quantity: Int,
): String = runBlocking { getPluralString(resource, quantity, quantity) }

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
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_title)).assertIsDisplayed()
    }

    @Test
    fun backButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.settings_back)).assertIsDisplayed()
    }

    @Test
    fun backButton_invokesCallback() {
        var backClicked = false
        setContent(onBack = { backClicked = true })

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.settings_back)).performClick()

        assertTrue("onBack should have been called", backClicked)
    }

    @Test
    fun offlineSection_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_offline_section)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_downloaded_headlines)).assertIsDisplayed()
    }

    @Test
    fun positiveCount_showsHeadlinesAvailableOffline() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 37))
        composeTestRule
            .onNodeWithText(pluralString(Res.plurals.settings_downloaded_headlines_count, 37))
            .assertIsDisplayed()
    }

    @Test
    fun singleCount_usesSingularCopy() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule
            .onNodeWithText(pluralString(Res.plurals.settings_downloaded_headlines_count, 1))
            .assertIsDisplayed()
    }

    @Test
    fun emptyCache_showsNoDownloadedHeadlines() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 0))
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_no_downloaded_headlines)).assertIsDisplayed()
    }

    @Test
    fun clearAction_isDisplayedInOfflineSection() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).assertIsDisplayed()
    }

    @Test
    fun clearAction_disabledForZeroCount() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 0))
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).assertIsNotEnabled()
    }

    @Test
    fun clearAction_enabledForPositiveCount() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).assertIsEnabled()
    }

    @Test
    fun clearActionClick_opensConfirmationDialog() {
        setContent(uiState = SettingsUiState(downloadedHeadlineCount = 1))
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_confirmation_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_confirmation_message)).assertIsDisplayed()
    }

    @Test
    fun cancelInConfirmationDialog_doesNotClear() {
        var clearClicked = false
        setContent(
            uiState = SettingsUiState(downloadedHeadlineCount = 1),
            onClearDownloadedHeadlines = { clearClicked = true },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_cancel)).performClick()

        assertTrue("clear callback should not fire on cancel", !clearClicked)
    }

    @Test
    fun confirmInConfirmationDialog_clears() {
        var clearClicked = false
        setContent(
            uiState = SettingsUiState(downloadedHeadlineCount = 1),
            onClearDownloadedHeadlines = { clearClicked = true },
        )

        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear_downloaded_headlines)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.settings_clear)).performClick()

        assertTrue("clear callback should fire on confirm", clearClicked)
    }
}
