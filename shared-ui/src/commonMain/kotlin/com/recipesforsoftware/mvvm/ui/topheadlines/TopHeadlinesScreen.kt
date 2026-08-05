package com.recipesforsoftware.mvvm.ui.topheadlines

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.FeedSource
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefSpacing
import com.recipesforsoftware.mvvm.ui.topheadlines.components.ArticleCard
import com.recipesforsoftware.mvvm.ui.topheadlines.components.CacheNoticeBanner
import com.recipesforsoftware.mvvm.ui.topheadlines.components.SkeletonArticleCard

private const val SKELETON_CARD_COUNT = 5

/**
 * Shared Top Headlines screen rendered identically on Android and iOS.
 *
 * Stateless: receives the current [TopHeadlinesUiState] and user callbacks from
 * the host, renders every state, and never fetches data itself. [onArticleClick]
 * is invoked when the user taps any article card; the host decides how to open
 * [Article.url] (for example through a platform URI handler). [topBarActions] is
 * an optional host-provided slot (for example the Android dark-mode menu); it
 * defaults to nothing so both platforms render the same core screen.
 *
 * Layout is responsive: content is capped at [SignalBriefSpacing.maxContentWidth]
 * and centered so wide screens (tablets, foldables) keep a comfortable reading
 * measure instead of stretching cards edge to edge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopHeadlinesScreen(
    uiState: TopHeadlinesUiState,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = TopHeadlinesStrings.TOP_BAR_TITLE,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = TopHeadlinesStrings.TOP_BAR_SUBTITLE,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = TopHeadlinesStrings.REFRESH,
                        )
                    }
                    topBarActions()
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (uiState) {
                TopHeadlinesUiState.Loading -> LoadingContent()
                is TopHeadlinesUiState.Success -> SuccessContent(uiState, onArticleClick)
                TopHeadlinesUiState.Empty -> EmptyContent(onRetry = onRefresh)
                is TopHeadlinesUiState.Error -> ErrorContent(error = uiState.error, onRetry = onRefresh)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = SignalBriefSpacing.maxContentWidth)
                .verticalScroll(rememberScrollState())
                .clearAndSetSemantics {
                    contentDescription = TopHeadlinesStrings.LOADING
                },
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
    ) {
        repeat(SKELETON_CARD_COUNT) {
            SkeletonArticleCard(
                modifier = Modifier.padding(horizontal = SignalBriefSpacing.pageHorizontal),
            )
        }
    }
}

@Composable
private fun SuccessContent(
    uiState: TopHeadlinesUiState.Success,
    onArticleClick: (Article) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth),
    ) {
        if (uiState.source == FeedSource.CACHE) {
            CacheNoticeBanner(
                modifier =
                    Modifier.padding(
                        horizontal = SignalBriefSpacing.pageHorizontal,
                        vertical = SignalBriefSpacing.m,
                    ),
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
        ) {
            items(
                items = uiState.articles,
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
                    modifier = Modifier.padding(horizontal = SignalBriefSpacing.pageHorizontal),
                )
            }
        }
    }
}

@Composable
private fun EmptyContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = TopHeadlinesStrings.EMPTY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = TopHeadlinesStrings.EMPTY_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(TopHeadlinesStrings.RETRY)
        }
    }
}

@Composable
private fun ErrorContent(
    error: TopHeadlinesError,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.l, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        )
        Text(
            text = TopHeadlinesStrings.ERROR_TITLE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = TopHeadlinesStrings.errorBody(error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onRetry) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(TopHeadlinesStrings.RETRY)
        }
    }
}
