package pl.recipesforsoftware.signalbrief.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
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
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

/**
 * Shared Settings screen rendered identically on Android, iOS, and Web.
 *
 * Stateless apart from the transient clearance confirmation dialog: receives
 * the current [SettingsUiState], an [onBack] callback, and an
 * [onClearDownloadedHeadlines] callback that performs the actual local cache
 * clear. Renders the settings title bar, an Offline section with the downloaded
 * headlines count, and a destructive clear action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onClearDownloadedHeadlines: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        SettingsContent(
            uiState = uiState,
            onRequestClear = { showClearConfirmation = true },
            contentPadding = contentPadding,
        )
    }

    if (showClearConfirmation) {
        ClearDownloadedHeadlinesConfirmationDialog(
            onConfirm = {
                showClearConfirmation = false
                onClearDownloadedHeadlines()
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onRequestClear: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = SignalBriefSpacing.maxContentWidth),
        ) {
            OfflineSectionTitle()
            SettingsOfflineRow(
                downloadedHeadlineCount = uiState.downloadedHeadlineCount,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SignalBriefSpacing.pageHorizontal),
            )
            ClearDownloadedHeadlinesButton(
                onClick = onRequestClear,
                enabled = uiState.downloadedHeadlineCount > 0,
            )
        }
    }
}

@Composable
private fun OfflineSectionTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.settings_offline_section),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SignalBriefSpacing.pageHorizontal,
                    vertical = SignalBriefSpacing.s,
                ),
    )
}

@Composable
private fun ClearDownloadedHeadlinesButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = SignalBriefSpacing.pageHorizontal),
    ) {
        Text(
            text = stringResource(Res.string.settings_clear_downloaded_headlines),
        )
    }
}

@Composable
private fun ClearDownloadedHeadlinesConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_clear_confirmation_title)) },
        text = { Text(stringResource(Res.string.settings_clear_confirmation_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.settings_clear),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.settings_cancel))
            }
        },
    )
}

@Composable
private fun SettingsOfflineRow(
    downloadedHeadlineCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.settings_downloaded_headlines),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = offlineCountCopy(downloadedHeadlineCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun offlineCountCopy(count: Int): String =
    if (count == 0) {
        stringResource(Res.string.settings_no_downloaded_headlines)
    } else {
        pluralStringResource(Res.plurals.settings_downloaded_headlines_count, count, count)
    }
