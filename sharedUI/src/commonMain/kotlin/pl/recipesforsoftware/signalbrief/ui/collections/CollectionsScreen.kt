package pl.recipesforsoftware.signalbrief.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_cancel
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_confirm_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete_message
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_delete_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_empty_description
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_error_invalid_name
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_error_not_found
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_error_unknown
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_name_label
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_new_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_options
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_rename
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_rename_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_save
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.collections_title
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

private val SigbyStateSize = 120.dp

/** Stateless shared Collections management screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    uiState: CollectionsUiState,
    onOpenCreateEditor: () -> Unit,
    onOpenRenameEditor: (Collection) -> Unit,
    onUpdateEditorName: (String) -> Unit,
    onConfirmEditor: () -> Unit,
    onDismissEditor: () -> Unit,
    onOpenDeleteConfirmation: (Collection) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onDismissError: () -> Unit,
    onOpenCollection: (Collection) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(uiState.error) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.collections_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.collections_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCreateEditor) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.collections_create))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (uiState.collections.isEmpty()) {
                EmptyContent(onOpenCreateEditor)
            } else {
                CollectionsList(uiState.collections, onOpenCollection, onOpenRenameEditor, onOpenDeleteConfirmation)
            }
        }
    }
    uiState.editor?.let { editor ->
        CollectionEditorDialog(
            editor,
            uiState.isSubmitting,
            onUpdateEditorName,
            onConfirmEditor,
            onDismissEditor,
        )
    }
    uiState.collectionPendingDeletion?.let { collection ->
        DeleteConfirmationDialog(
            collection,
            uiState.isSubmitting,
            onConfirmDelete,
            onDismissDeleteConfirmation,
        )
    }
}

@Composable
private fun CollectionsList(
    collections: List<Collection>,
    onOpen: (Collection) -> Unit,
    onRename: (Collection) -> Unit,
    onDelete: (Collection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth),
        contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxxl),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.xs),
    ) {
        items(items = collections, key = Collection::id) { collection ->
            CollectionRow(collection, { onOpen(collection) }, { onRename(collection) }, { onDelete(collection) })
        }
    }
}

@Composable
private fun CollectionRow(
    collection: Collection,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onOpen)
                .padding(horizontal = SignalBriefSpacing.pageHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(collection.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.collections_options))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.collections_rename)) },
                onClick = {
                    menuExpanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.collections_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EmptyContent(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
    ) {
        Sigby(SigbyVariant.Compact, Modifier.size(SigbyStateSize), null)
        Text(
            stringResource(Res.string.collections_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.collections_empty_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onCreate) { Text(stringResource(Res.string.collections_create)) }
    }
}

@Composable
private fun CollectionEditorDialog(
    editor: CollectionsEditor,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isCreate = editor is CollectionsEditor.Create
    val title =
        if (isCreate) {
            stringResource(Res.string.collections_new_title)
        } else {
            stringResource(Res.string.collections_rename_title)
        }
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(Res.string.collections_name_label)) },
                singleLine = true,
                enabled = !isSubmitting,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text(
                    if (isCreate) {
                        stringResource(Res.string.collections_confirm_create)
                    } else {
                        stringResource(Res.string.collections_save)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(stringResource(Res.string.collections_cancel))
            }
        },
    )
}

@Composable
private fun DeleteConfirmationDialog(
    collection: Collection,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(Res.string.collections_delete_title)) },
        text = { Text(stringResource(Res.string.collections_delete_message, collection.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text(stringResource(Res.string.collections_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(stringResource(Res.string.collections_cancel))
            }
        },
    )
}

private fun CollectionsError.messageResource(): StringResource =
    when (this) {
        CollectionsError.InvalidName -> Res.string.collections_error_invalid_name
        CollectionsError.NotFound -> Res.string.collections_error_not_found
        CollectionsError.Unknown -> Res.string.collections_error_unknown
    }
