package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicMonitoringScreen(
    uiState: TopicMonitoringUiState,
    onOpenCreateEditor: () -> Unit,
    onOpenRenameEditor: (MonitoredTopic) -> Unit,
    onUpdateEditorQuery: (String) -> Unit,
    onConfirmEditor: () -> Unit,
    onDismissEditor: () -> Unit,
    onOpenDeleteConfirmation: (MonitoredTopic) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onDismissError: () -> Unit,
    onOpenTopicMatches: (MonitoredTopic) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error, uiState.editor) {
        if (uiState.editor == null) {
            uiState.error?.let {
                snackbar.showSnackbar(TopicMonitoringStrings.error(it))
                onDismissError()
            }
        }
    }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = {
            Text(TopicMonitoringStrings.TITLE, fontWeight = FontWeight.SemiBold)
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, TopicMonitoringStrings.BACK)
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = onOpenCreateEditor) {
            Icon(Icons.Filled.Add, TopicMonitoringStrings.CREATE_TOPIC)
        }
    }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        TopicMonitoringContent(
            padding,
            uiState,
            onOpenCreateEditor,
            onOpenTopicMatches,
            onOpenRenameEditor,
            onOpenDeleteConfirmation,
        )
    }
    uiState.editor?.let {
        TopicEditorDialog(
            it,
            uiState.isCreating || (it as? TopicEditor.Rename)?.topic?.id in uiState.mutatingTopicIds,
            uiState.error,
            onUpdateEditorQuery,
            onConfirmEditor,
            onDismissEditor,
        )
    }
    uiState.pendingDelete?.let {
        DeleteTopicDialog(
            it,
            it.id in uiState.mutatingTopicIds,
            onConfirmDelete,
            onDismissDeleteConfirmation,
        )
    }
}

@Composable private fun EmptyTopics(
    modifier: Modifier,
    create: () -> Unit,
) = Column(
    modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
) {
    Text(
        TopicMonitoringStrings.EMPTY_TITLE,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        TopicMonitoringStrings.EMPTY_DESCRIPTION,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = create) { Text(TopicMonitoringStrings.CREATE_TOPIC) }
}

@Composable private fun TopicMonitoringContent(
    padding: PaddingValues,
    uiState: TopicMonitoringUiState,
    onOpenCreateEditor: () -> Unit,
    onOpenTopicMatches: (MonitoredTopic) -> Unit,
    onOpenRenameEditor: (MonitoredTopic) -> Unit,
    onOpenDeleteConfirmation: (MonitoredTopic) -> Unit,
) {
    if (uiState.topics.isEmpty()) {
        EmptyTopics(
            Modifier.padding(padding),
            onOpenCreateEditor,
        )
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).widthIn(max = SignalBriefSpacing.maxContentWidth),
            contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxxl),
        ) {
            topicRows(uiState, onOpenTopicMatches, onOpenRenameEditor, onOpenDeleteConfirmation)
        }
    }
}

private fun LazyListScope.topicRows(
    uiState: TopicMonitoringUiState,
    open: (MonitoredTopic) -> Unit,
    rename: (MonitoredTopic) -> Unit,
    delete: (MonitoredTopic) -> Unit,
) {
    items(uiState.topics, MonitoredTopic::id) {
        TopicRow(
            it,
            uiState.matchCountsByTopicId[it.id] ?: 0,
            uiState.hasLocalArticles,
            open,
            rename,
            delete,
        )
    }
}

@Composable private fun TopicRow(
    topic: MonitoredTopic,
    matchCount: Int,
    hasLocalArticles: Boolean,
    open: (MonitoredTopic) -> Unit,
    rename: (MonitoredTopic) -> Unit,
    delete: (MonitoredTopic) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val summary =
        when {
            hasLocalArticles -> TopicMonitoringStrings.matchSummary(matchCount)
            else -> TopicMonitoringStrings.NO_DOWNLOADED_HEADLINES
        }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = SignalBriefSpacing.pageHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clickable { open(topic) },
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                topic.query,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = {
            expanded =
                true
        }) { Icon(Icons.Filled.MoreVert, TopicMonitoringStrings.OPTIONS) }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text(TopicMonitoringStrings.RENAME) }, {
                expanded =
                    false
                ; rename(topic)
            })
            ; DropdownMenuItem({ Text(TopicMonitoringStrings.DELETE, color = MaterialTheme.colorScheme.error) }, {
                expanded =
                    false
                ; delete(topic)
            })
        }
    }
}

@Composable private fun TopicEditorDialog(
    editor: TopicEditor,
    submitting: Boolean,
    error: TopicMonitoringUiError?,
    change: (String) -> Unit,
    confirm: () -> Unit,
    dismiss: () -> Unit,
) {
    val create = editor is TopicEditor.Create
    AlertDialog(onDismissRequest = {
        if (!submitting) dismiss()
    }, title = {
        Text(if (create) TopicMonitoringStrings.NEW_TOPIC else TopicMonitoringStrings.RENAME_TOPIC)
    }, text = {
        Column {
            OutlinedTextField(editor.query, change, label = {
                Text(TopicMonitoringStrings.TOPIC_QUERY)
            }, singleLine = true, enabled = !submitting)
            error?.let {
                Text(
                    TopicMonitoringStrings.error(it),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }, confirmButton = {
        TextButton(confirm, enabled = !submitting) {
            Text(if (create) TopicMonitoringStrings.CREATE else TopicMonitoringStrings.SAVE)
        }
    }, dismissButton = { TextButton(dismiss, enabled = !submitting) { Text(TopicMonitoringStrings.CANCEL) } })
}

@Composable private fun DeleteTopicDialog(
    topic: MonitoredTopic,
    submitting: Boolean,
    confirm: () -> Unit,
    dismiss: () -> Unit,
) = AlertDialog(onDismissRequest = {
    if (!submitting) dismiss()
}, title = {
    Text(TopicMonitoringStrings.DELETE_TITLE)
}, text = {
    Text("${TopicMonitoringStrings.DELETE_MESSAGE} ${topic.query}.")
}, confirmButton = {
    TextButton(confirm, enabled = !submitting) {
        Text(TopicMonitoringStrings.DELETE, color = MaterialTheme.colorScheme.error)
    }
}, dismissButton = { TextButton(dismiss, enabled = !submitting) { Text(TopicMonitoringStrings.CANCEL) } })
