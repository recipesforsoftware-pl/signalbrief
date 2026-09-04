package pl.recipesforsoftware.signalbrief.ui.collections

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
import pl.recipesforsoftware.signalbrief.domain.model.Collection
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(CollectionsStrings.error(it))
            onDismissError()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(CollectionsStrings.TITLE, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = CollectionsStrings.BACK)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCreateEditor) {
                Icon(Icons.Filled.Add, contentDescription = CollectionsStrings.CREATE_COLLECTION)
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
                CollectionsList(uiState.collections, onOpenRenameEditor, onOpenDeleteConfirmation)
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
    onRename: (Collection) -> Unit,
    onDelete: (Collection) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth),
        contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxxl),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.xs),
    ) {
        items(items = collections, key = Collection::id) { collection ->
            CollectionRow(collection, { onRename(collection) }, { onDelete(collection) })
        }
    }
}

@Composable
private fun CollectionRow(
    collection: Collection,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = SignalBriefSpacing.pageHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(collection.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { menuExpanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = CollectionsStrings.OPTIONS)
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(CollectionsStrings.RENAME) },
                onClick = {
                    menuExpanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = { Text(CollectionsStrings.DELETE, color = MaterialTheme.colorScheme.error) },
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
            CollectionsStrings.EMPTY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            CollectionsStrings.EMPTY_DESCRIPTION,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onCreate) { Text(CollectionsStrings.CREATE_COLLECTION) }
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
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(if (isCreate) CollectionsStrings.NEW_COLLECTION else CollectionsStrings.RENAME_COLLECTION) },
        text = {
            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text(CollectionsStrings.COLLECTION_NAME) },
                singleLine = true,
                enabled = !isSubmitting,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text(if (isCreate) CollectionsStrings.CREATE else CollectionsStrings.SAVE)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text(CollectionsStrings.CANCEL) }
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
        title = { Text(CollectionsStrings.DELETE_TITLE) },
        text = { Text("${CollectionsStrings.DELETE_MESSAGE} ${collection.name}") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSubmitting) {
                Text(CollectionsStrings.DELETE, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text(CollectionsStrings.CANCEL) }
        },
    )
}
