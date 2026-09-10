package pl.recipesforsoftware.signalbrief.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

/**
 * Shared Settings screen rendered identically on Android, iOS, and Web.
 *
 * Stateless: receives the current [SettingsUiState] and a [onBack] callback.
 * Renders the settings title bar, an Offline section heading, and a single
 * compact row for downloaded headlines count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = SettingsStrings.TOP_BAR_TITLE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = SettingsStrings.BACK,
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
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
                Text(
                    text = SettingsStrings.OFFLINE_SECTION,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SignalBriefSpacing.pageHorizontal,
                                vertical = SignalBriefSpacing.s,
                            ),
                )

                SettingsOfflineRow(
                    downloadedHeadlineCount = uiState.downloadedHeadlineCount,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SignalBriefSpacing.pageHorizontal),
                )
            }
        }
    }
}

@Composable
private fun SettingsOfflineRow(
    downloadedHeadlineCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = SettingsStrings.DOWNLOADED_HEADLINES,
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

private fun offlineCountCopy(count: Int): String =
    if (count == 0) {
        SettingsStrings.NO_DOWNLOADED_HEADLINES
    } else {
        SettingsStrings.downloadedHeadlinesCount(count)
    }
