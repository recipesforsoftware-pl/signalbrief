package pl.recipesforsoftware.signalbrief.ui.articledetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SignalBriefPrimaryButton
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefShapes
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleHeader
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.BookmarkIcons
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.SignalBriefArticleThumbnail
import pl.recipesforsoftware.signalbrief.ui.topheadlines.hasActionableUrl

private val DetailsImageHeight = 220.dp

/**
 * Shared Article Details screen rendered identically on Android and iOS.
 *
 * Renders the locally available [ArticleDetailsUiState.article] snapshot only;
 * nothing is fetched or enriched here. Optional fields that are absent are
 * omitted gracefully instead of being replaced with placeholders, and the raw
 * URL is never printed as body content. The bookmark action mirrors
 * [ArticleDetailsUiState.isSaved], which is persistence-derived, and
 * [onOpenFullArticle] is the only path to the external browser.
 *
 * Layout is responsive: content is capped at [SignalBriefSpacing.maxContentWidth]
 * and centered so wide screens keep a comfortable reading measure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailsScreen(
    uiState: ArticleDetailsUiState,
    onBack: () -> Unit,
    onBookmarkClick: () -> Unit,
    onOpenFullArticle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ArticleDetailsStrings.BACK,
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = SignalBriefSpacing.maxContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        ) {
            val article = uiState.article
            val imageUrl = article.imageUrl
            if (imageUrl != null) {
                DetailsImage(imageUrl = imageUrl)
            }
            DetailsBody(
                uiState = uiState,
                onBookmarkClick = onBookmarkClick,
                onOpenFullArticle = onOpenFullArticle,
            )
        }
    }
}

@Composable
private fun DetailsImage(imageUrl: String) {
    SignalBriefArticleThumbnail(
        imageReference = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DetailsImageHeight)
                .clip(SignalBriefShapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun DetailsBody(
    uiState: ArticleDetailsUiState,
    onBookmarkClick: () -> Unit,
    onOpenFullArticle: () -> Unit,
) {
    val article = uiState.article
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SignalBriefSpacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
    ) {
        Spacer(modifier = Modifier.height(SignalBriefSpacing.s))
        article.source?.name?.let { sourceName ->
            ArticleHeader(sourceName = sourceName)
        }
        article.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        article.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (article.hasActionableUrl()) {
            Spacer(modifier = Modifier.height(SignalBriefSpacing.s))
            ReadFullArticleAction(onClick = onOpenFullArticle)
            BookmarkAction(
                isSaved = uiState.isSaved,
                onClick = onBookmarkClick,
            )
        }
        Spacer(modifier = Modifier.height(SignalBriefSpacing.xxl))
    }
}

@Composable
private fun ReadFullArticleAction(onClick: () -> Unit) {
    SignalBriefPrimaryButton(
        text = ArticleDetailsStrings.READ_FULL_ARTICLE,
        onClick = onClick,
    )
}

@Composable
private fun BookmarkAction(
    isSaved: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
    ) {
        Icon(
            imageVector =
                if (isSaved) {
                    BookmarkIcons.Filled
                } else {
                    BookmarkIcons.Outlined
                },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.size(SignalBriefSpacing.s))
        Text(
            text =
                if (isSaved) {
                    TopHeadlinesStrings.BOOKMARK_REMOVE
                } else {
                    TopHeadlinesStrings.BOOKMARK_SAVE
                },
        )
    }
}
