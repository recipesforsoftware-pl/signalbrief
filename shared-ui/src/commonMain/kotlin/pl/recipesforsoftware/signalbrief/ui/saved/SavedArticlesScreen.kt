package pl.recipesforsoftware.signalbrief.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

private val SigbyStateSize = 120.dp

/**
 * Shared Saved Articles screen rendered identically on Android and iOS.
 *
 * Stateless: receives the current [SavedArticlesUiState] and user callbacks from
 * the host, renders every state, and never fetches data itself. Every rendered
 * article is already saved, so the bookmark action always offers removal.
 *
 * Layout is responsive: content is capped at [SignalBriefSpacing.maxContentWidth]
 * and centered so wide screens (tablets, foldables) keep a comfortable reading
 * measure instead of stretching cards edge to edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedArticlesScreen(
    uiState: SavedArticlesUiState,
    onArticleClick: (Article) -> Unit,
    onRemoveClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = SavedArticlesStrings.TOP_BAR_TITLE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        },
        bottomBar = bottomBar,
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                SavedArticlesUiState.Loading -> {
                    LoadingContent()
                }

                is SavedArticlesUiState.Content -> {
                    ContentList(
                        articles = uiState.articles,
                        onArticleClick = onArticleClick,
                        onRemoveClick = onRemoveClick,
                    )
                }

                SavedArticlesUiState.Empty -> {
                    EmptyContent()
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clearAndSetSemantics {
                    contentDescription = "Loading saved articles..."
                },
    )
}

@Composable
private fun ContentList(
    articles: List<Article>,
    onArticleClick: (Article) -> Unit,
    onRemoveClick: (Article) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth),
        contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
    ) {
        items(
            items = articles,
            key = { article -> article.url },
        ) { article ->
            ArticleCard(
                article = article,
                onClick =
                    if (article.hasActionableUrl()) {
                        { onArticleClick(article) }
                    } else {
                        null
                    },
                isSaved = true,
                onBookmarkClick =
                    if (article.hasActionableUrl()) {
                        { onRemoveClick(article) }
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = SignalBriefSpacing.pageHorizontal),
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
    ) {
        Sigby(
            variant = SigbyVariant.Compact,
            contentDescription = null,
            modifier = Modifier.size(SigbyStateSize),
        )
        Text(
            text = SavedArticlesStrings.EMPTY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = SavedArticlesStrings.EMPTY_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
