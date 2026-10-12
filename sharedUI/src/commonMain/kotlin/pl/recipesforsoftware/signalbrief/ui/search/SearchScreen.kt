package pl.recipesforsoftware.signalbrief.ui.search

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_idle_subtitle
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_idle_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_loading
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_monitored_topics
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_local_articles_subtitle
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_local_articles_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_results_subtitle
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_no_results_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.search_title
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

private val SigbyStateSize = 120.dp

/**
 * Shared Local Search screen rendered identically on Android and iOS.
 *
 * Stateless: receives the current query, [SearchUiState], and user callbacks
 * from the host, renders every state, and never fetches data itself. Search
 * operates only over the locally cached headlines supplied by the ViewModel.
 *
 * The search field requests focus when the screen first appears so the user
 * can start typing immediately. Result cards reuse [ArticleCard] and mirror
 * the feed bookmark behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    uiState: SearchUiState,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: ((Article) -> Unit)?,
    onOpenTopicMonitoring: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { SearchTopBar(onBack = onBack, onOpenTopicMonitoring = onOpenTopicMonitoring) },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = SignalBriefSpacing.maxContentWidth)
                        .padding(horizontal = SignalBriefSpacing.pageHorizontal)
                        .padding(top = SignalBriefSpacing.s),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (uiState) {
                    SearchUiState.Loading -> {
                        LoadingContent()
                    }

                    SearchUiState.Idle -> {
                        IdleContent()
                    }

                    SearchUiState.NoLocalArticles -> {
                        NoLocalArticlesContent()
                    }

                    is SearchUiState.NoResults -> {
                        NoResultsContent(uiState.query)
                    }

                    is SearchUiState.Results -> {
                        ResultsContent(
                            uiState = uiState,
                            onArticleClick = onArticleClick,
                            onBookmarkClick = onBookmarkClick,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    onBack: () -> Unit,
    onOpenTopicMonitoring: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.search_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.search_back),
                )
            }
        },
        actions = {
            TextButton(onClick = onOpenTopicMonitoring) {
                Text(stringResource(Res.string.search_monitored_topics))
            }
        },
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = { Text(stringResource(Res.string.search_headlines)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        singleLine = true,
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ResultsContent(
    uiState: SearchUiState.Results,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: ((Article) -> Unit)?,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = SignalBriefSpacing.maxContentWidth),
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

@Composable
private fun LoadingContent() {
    val loadingDescription = stringResource(Res.string.search_loading)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clearAndSetSemantics {
                    contentDescription = loadingDescription
                },
    )
}

@Composable
private fun IdleContent() {
    MessageContent(
        title = stringResource(Res.string.search_idle_title),
        subtitle = stringResource(Res.string.search_idle_subtitle),
    )
}

@Composable
private fun NoResultsContent(query: String) {
    MessageContent(
        title = stringResource(Res.string.search_no_results_title),
        subtitle = stringResource(Res.string.search_no_results_subtitle, query),
    )
}

@Composable
private fun NoLocalArticlesContent() {
    MessageContent(
        title = stringResource(Res.string.search_no_local_articles_title),
        subtitle = stringResource(Res.string.search_no_local_articles_subtitle),
    )
}

@Composable
private fun MessageContent(
    title: String,
    subtitle: String,
) {
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
