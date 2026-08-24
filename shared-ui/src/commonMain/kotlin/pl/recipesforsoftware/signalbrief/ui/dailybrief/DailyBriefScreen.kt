package pl.recipesforsoftware.signalbrief.ui.dailybrief

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBriefScreen(
    uiState: DailyBriefUiState,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(
            title = {
                Text(
                    text = DailyBriefStrings.TOP_BAR_TITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            },
        )
    }, bottomBar = bottomBar) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when (uiState) {
                DailyBriefUiState.Loading -> Unit
                DailyBriefUiState.Empty -> EmptyContent()
                is DailyBriefUiState.Content -> ContentList(uiState, onArticleClick, onBookmarkClick)
            }
        }
    }
}

@Composable private fun ContentList(
    state: DailyBriefUiState.Content,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth),
        contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
    ) {
        item {
            Text(
                DailyBriefStrings.INTRO,
                modifier =
                    Modifier.padding(
                        horizontal = SignalBriefSpacing.pageHorizontal,
                        vertical = SignalBriefSpacing.m,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.articles, key = { it.url }) { article ->
            ArticleCard(
                article,
                onClick = { onArticleClick(article) },
                isSaved =
                    article.url in state.savedUrls,
                onBookmarkClick = { onBookmarkClick(article) },
                modifier = Modifier.padding(horizontal = SignalBriefSpacing.pageHorizontal),
            )
        }
    }
}

@Composable private fun EmptyContent() {
    Column(
        Modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
    ) {
        Sigby(
            variant = SigbyVariant.Compact,
            modifier = Modifier.size(120.dp),
            contentDescription = null,
        )
        Text(
            DailyBriefStrings.EMPTY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            DailyBriefStrings.EMPTY_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
