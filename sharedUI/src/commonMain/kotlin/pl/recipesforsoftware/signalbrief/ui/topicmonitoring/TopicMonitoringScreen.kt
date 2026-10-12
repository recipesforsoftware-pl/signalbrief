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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_cancel
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_confirm_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete_message
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_empty_description
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_duplicate_query
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_invalid_query
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_not_found
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_unknown
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_match_summary
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_new_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_no_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_options
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_query_label
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_rename
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_rename_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_save
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_title
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
    val errorMessage = uiState.error?.let { stringResource(it.messageResource()) }
    LaunchedEffect(uiState.error, uiState.editor) {
        if (uiState.editor == null) {
            errorMessage?.let {
                snackbar.showSnackbar(it)
                onDismissError()
            }
        }
    }
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = {
            Text(stringResource(Res.string.topic_monitoring_title), fontWeight = FontWeight.SemiBold)
        }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.topic_monitoring_back))
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = onOpenCreateEditor) {
            Icon(Icons.Filled.Add, stringResource(Res.string.topic_monitoring_create))
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
        stringResource(Res.string.topic_monitoring_empty_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Text(
        stringResource(Res.string.topic_monitoring_empty_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    TextButton(onClick = create) { Text(stringResource(Res.string.topic_monitoring_create)) }
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
            hasLocalArticles -> pluralStringResource(Res.plurals.topic_monitoring_match_summary, matchCount, matchCount)
            else -> stringResource(Res.string.topic_monitoring_no_downloaded_headlines)
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
        }) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.topic_monitoring_options)) }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text(stringResource(Res.string.topic_monitoring_rename)) }, {
                expanded =
                    false
                ; rename(topic)
            })
            ; DropdownMenuItem(
                { Text(stringResource(Res.string.topic_monitoring_delete), color = MaterialTheme.colorScheme.error) },
                {
                    expanded =
                        false
                    ; delete(topic)
                },
            )
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
    val title =
        if (create) {
            stringResource(Res.string.topic_monitoring_new_title)
        } else {
            stringResource(Res.string.topic_monitoring_rename_title)
        }
    val confirmLabel =
        if (create) {
            stringResource(Res.string.topic_monitoring_confirm_create)
        } else {
            stringResource(Res.string.topic_monitoring_save)
        }
    AlertDialog(
        onDismissRequest = {
            if (!submitting) dismiss()
        },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = editor.query,
                    onValueChange = change,
                    label = { Text(stringResource(Res.string.topic_monitoring_query_label)) },
                    singleLine = true,
                    enabled = !submitting,
                )
                error?.let {
                    Text(
                        stringResource(it.messageResource()),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(confirm, enabled = !submitting) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(dismiss, enabled = !submitting) {
                Text(stringResource(Res.string.topic_monitoring_cancel))
            }
        },
    )
}

@Composable private fun DeleteTopicDialog(
    topic: MonitoredTopic,
    submitting: Boolean,
    confirm: () -> Unit,
    dismiss: () -> Unit,
) = AlertDialog(onDismissRequest = {
    if (!submitting) dismiss()
}, title = {
    Text(stringResource(Res.string.topic_monitoring_delete_title))
}, text = {
    Text(stringResource(Res.string.topic_monitoring_delete_message, topic.query))
}, confirmButton = {
    TextButton(confirm, enabled = !submitting) {
        Text(stringResource(Res.string.topic_monitoring_delete), color = MaterialTheme.colorScheme.error)
    }
}, dismissButton = {
    TextButton(dismiss, enabled = !submitting) {
        Text(stringResource(Res.string.topic_monitoring_cancel))
    }
})

private fun TopicMonitoringUiError.messageResource(): StringResource =
    when (this) {
        TopicMonitoringUiError.InvalidQuery -> Res.string.topic_monitoring_error_invalid_query
        TopicMonitoringUiError.DuplicateQuery -> Res.string.topic_monitoring_error_duplicate_query
        TopicMonitoringUiError.NotFound -> Res.string.topic_monitoring_error_not_found
        TopicMonitoringUiError.Unknown -> Res.string.topic_monitoring_error_unknown
    }
