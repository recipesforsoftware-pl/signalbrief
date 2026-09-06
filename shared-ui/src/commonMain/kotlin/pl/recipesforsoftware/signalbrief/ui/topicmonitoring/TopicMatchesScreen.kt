package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

private val SigbyStateSize = 120.dp

/**
 * Shared Topic Matches screen rendered identically on Android and iOS.
 *
 * Stateless: receives the selected [MonitoredTopic] and [TopicMatchesUiState]
 * from the host and renders every state. Result cards reuse [ArticleCard] and
 * mirror the Search/Headlines bookmark behavior. The selected topic is shown as
 * the top-app-bar title so the user always knows which monitor they are viewing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicMatchesScreen(
    uiState: TopicMatchesUiState,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: ((Article) -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopicMatchesTopBar(topic = uiState.topic, onBack = onBack) },
    ) { contentPadding ->
        when (uiState) {
            is TopicMatchesUiState.Loading -> {
                LoadingContent(contentPadding)
            }

            is TopicMatchesUiState.NoLocalArticles -> {
                MessageContent(contentPadding) {
                    Message(
                        title = TopicMatchesStrings.NO_LOCAL_ARTICLES_TITLE,
                        subtitle = TopicMatchesStrings.NO_LOCAL_ARTICLES_SUBTITLE,
                    )
                }
            }

            is TopicMatchesUiState.NoMatches -> {
                MessageContent(contentPadding) {
                    Message(
                        title = TopicMatchesStrings.NO_MATCHES_TITLE,
                        subtitle = TopicMatchesStrings.NO_MATCHES_SUBTITLE,
                    )
                }
            }

            is TopicMatchesUiState.Content -> {
                ResultsContent(
                    uiState = uiState,
                    contentPadding = contentPadding,
                    onArticleClick = onArticleClick,
                    onBookmarkClick = onBookmarkClick,
                )
            }
        }
    }
}

private val TopicMatchesUiState.topic: MonitoredTopic
    get() =
        when (this) {
            is TopicMatchesUiState.Loading -> topic
            is TopicMatchesUiState.NoLocalArticles -> topic
            is TopicMatchesUiState.NoMatches -> topic
            is TopicMatchesUiState.Content -> topic
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicMatchesTopBar(
    topic: MonitoredTopic,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = topic.query,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = TopicMatchesStrings.BACK,
                )
            }
        },
    )
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clearAndSetSemantics {
                    contentDescription = "Loading topic matches..."
                },
    )
}

@Composable
private fun MessageContent(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun Message(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m, Alignment.CenterVertically),
    ) {
        Sigby(
            variant = SigbyVariant.Compact,
            contentDescription = null,
            modifier = Modifier.size(SigbyStateSize),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(SignalBriefSpacing.xxl))
    }
}

@Composable
private fun ResultsContent(
    uiState: TopicMatchesUiState.Content,
    contentPadding: PaddingValues,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: ((Article) -> Unit)?,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = SignalBriefSpacing.maxContentWidth)
                .padding(contentPadding),
        contentPadding = PaddingValues(bottom = SignalBriefSpacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
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
                isSaved = article.url in uiState.savedUrls,
                onBookmarkClick =
                    if (onBookmarkClick != null && article.hasActionableUrl()) {
                        { onBookmarkClick(article) }
                    } else {
                        null
                    },
                modifier = Modifier.padding(horizontal = SignalBriefSpacing.pageHorizontal),
            )
        }
    }
}
