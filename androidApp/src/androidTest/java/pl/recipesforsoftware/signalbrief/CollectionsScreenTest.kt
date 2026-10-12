package pl.recipesforsoftware.signalbrief

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
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete_message
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_name_label
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_new_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_options
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_rename
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_rename_title
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsEditor
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsScreen
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsUiState
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme

private fun resourceString(
    resource: StringResource,
    vararg args: Any,
): String = runBlocking { getString(resource, *args) }

class CollectionsScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    private fun setContent(
        state: CollectionsUiState,
        onCreate: () -> Unit = {},
        onOpenCollection: (Collection) -> Unit = {},
        onRename: (Collection) -> Unit = {},
        onDelete: (Collection) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                CollectionsScreen(
                    uiState = state,
                    onOpenCreateEditor = onCreate,
                    onOpenRenameEditor = onRename,
                    onUpdateEditorName = {},
                    onConfirmEditor = {},
                    onDismissEditor = {},
                    onOpenDeleteConfirmation = onDelete,
                    onConfirmDelete = {},
                    onDismissDeleteConfirmation = {},
                    onDismissError = {},
                    onOpenCollection = onOpenCollection,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun emptyState_displaysCreateAffordance() {
        setContent(CollectionsUiState())
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_empty_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_create)).assertIsDisplayed()
    }

    @Test
    fun populatedState_displaysCollections() {
        setContent(CollectionsUiState(collections = listOf(Collection("1", "Read later"))))
        composeTestRule.onNodeWithText("Read later").assertIsDisplayed()
    }

    @Test
    fun collectionRow_invokesOpenWithCorrectCollection() {
        val collection = Collection("42", "Read later")
        var opened: Collection? = null
        setContent(CollectionsUiState(collections = listOf(collection)), onOpenCollection = { opened = it })

        composeTestRule.onNodeWithText(collection.name).performClick()

        check(opened == collection)
    }

    @Test
    fun renameOverflowAction_doesNotOpenCollection() {
        val collection = Collection("42", "Read later")
        var opened = false
        var renamed = false
        setContent(
            CollectionsUiState(collections = listOf(collection)),
            onOpenCollection = { opened = true },
            onRename = { renamed = it == collection },
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.collections_options)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_rename)).performClick()

        check(renamed)
        check(!opened)
    }

    @Test
    fun deleteOverflowAction_doesNotOpenCollection() {
        val collection = Collection("42", "Read later")
        var opened = false
        var deleted = false
        setContent(
            CollectionsUiState(collections = listOf(collection)),
            onOpenCollection = { opened = true },
            onDelete = { deleted = it == collection },
        )

        composeTestRule.onNodeWithContentDescription(resourceString(Res.string.collections_options)).performClick()
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_delete)).performClick()

        check(deleted)
        check(!opened)
    }

    @Test
    fun createAffordance_invokesCallback() {
        var opened = false
        setContent(CollectionsUiState()) { opened = true }
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_create)).performClick()
        check(opened)
    }

    @Test
    fun createEditor_isDisplayed() {
        setContent(CollectionsUiState(editor = CollectionsEditor.Create()))
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_new_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_name_label)).assertIsDisplayed()
    }

    @Test
    fun renameEditor_displaysPrefilledCollectionName() {
        setContent(CollectionsUiState(editor = CollectionsEditor.Rename(Collection("1", "Reading"))))
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_rename_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading").assertIsDisplayed()
    }

    @Test
    fun deleteConfirmation_isDisplayed() {
        val collection = Collection("1", "Read later")
        setContent(CollectionsUiState(collectionPendingDeletion = collection))
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_delete_title)).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(resourceString(Res.string.collections_delete_message, collection.name))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(resourceString(Res.string.collections_delete)).assertIsDisplayed()
    }
}
