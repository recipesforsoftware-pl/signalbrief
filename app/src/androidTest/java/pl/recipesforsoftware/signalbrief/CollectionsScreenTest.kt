package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsEditor
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsStrings
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

class CollectionsScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun setContent(
        state: CollectionsUiState,
        onCreate: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                CollectionsScreen(
                    uiState = state,
                    onOpenCreateEditor = onCreate,
                    onOpenRenameEditor = {},
                    onUpdateEditorName = {},
                    onConfirmEditor = {},
                    onDismissEditor = {},
                    onOpenDeleteConfirmation = {},
                    onConfirmDelete = {},
                    onDismissDeleteConfirmation = {},
                    onDismissError = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun emptyState_displaysCreateAffordance() {
        setContent(CollectionsUiState())
        composeTestRule.onNodeWithText(CollectionsStrings.EMPTY_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(CollectionsStrings.CREATE_COLLECTION).assertIsDisplayed()
    }

    @Test
    fun populatedState_displaysCollections() {
        setContent(CollectionsUiState(collections = listOf(Collection("1", "Read later"))))
        composeTestRule.onNodeWithText("Read later").assertIsDisplayed()
    }

    @Test
    fun createAffordance_invokesCallback() {
        var opened = false
        setContent(CollectionsUiState()) { opened = true }
        composeTestRule.onNodeWithText(CollectionsStrings.CREATE_COLLECTION).performClick()
        check(opened)
    }

    @Test
    fun createEditor_isDisplayed() {
        setContent(CollectionsUiState(editor = CollectionsEditor.Create()))
        composeTestRule.onNodeWithText(CollectionsStrings.NEW_COLLECTION).assertIsDisplayed()
        composeTestRule.onNodeWithText(CollectionsStrings.COLLECTION_NAME).assertIsDisplayed()
    }

    @Test
    fun renameEditor_displaysPrefilledCollectionName() {
        setContent(CollectionsUiState(editor = CollectionsEditor.Rename(Collection("1", "Reading"))))
        composeTestRule.onNodeWithText(CollectionsStrings.RENAME_COLLECTION).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmation_isDisplayed() {
        setContent(CollectionsUiState(collectionPendingDeletion = Collection("1", "Read later")))
        composeTestRule.onNodeWithText(CollectionsStrings.DELETE_TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(CollectionsStrings.DELETE).assertIsDisplayed()
    }
}
