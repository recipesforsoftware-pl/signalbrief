package pl.recipesforsoftware.signalbrief.ui.articledetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

/** Stateless shared dialog for selecting the collections that contain an article. */
@Composable
fun ArticleCollectionAssignmentDialog(
    uiState: ArticleCollectionAssignmentUiState,
    onToggleCollection: (String) -> Unit,
    onDismiss: () -> Unit,
    onManageCollections: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(ArticleCollectionAssignmentStrings.TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.s)) {
                when {
                    uiState.isLoadingCollections -> LoadingContent()
                    uiState.collections.isEmpty() -> EmptyContent(onManageCollections)
                    else -> CollectionList(uiState, onToggleCollection)
                }
                uiState.error?.let { error ->
                    Text(
                        text = ArticleCollectionAssignmentStrings.error(error),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(ArticleCollectionAssignmentStrings.CLOSE) }
        },
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(SignalBriefSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(onManageCollections: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.s),
    ) {
        Text(
            ArticleCollectionAssignmentStrings.EMPTY_TITLE,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            ArticleCollectionAssignmentStrings.EMPTY_DESCRIPTION,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onManageCollections) {
            Text(ArticleCollectionAssignmentStrings.MANAGE_COLLECTIONS)
        }
    }
}

@Composable
private fun CollectionList(
    uiState: ArticleCollectionAssignmentUiState,
    onToggleCollection: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
        items(uiState.collections, key = Collection::id) { collection ->
            val checked = collection.id in uiState.selectedCollectionIds
            val isMutating = collection.id in uiState.mutatingCollectionIds
            ListItem(
                headlineContent = { Text(collection.name) },
                trailingContent = {
                    Checkbox(checked = checked, onCheckedChange = null, enabled = !isMutating)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { stateDescription = if (checked) "Selected" else "Not selected" }
                        .toggleable(
                            value = checked,
                            enabled = !isMutating,
                            role = Role.Checkbox,
                            onValueChange = { onToggleCollection(collection.id) },
                        ),
            )
        }
    }
}
