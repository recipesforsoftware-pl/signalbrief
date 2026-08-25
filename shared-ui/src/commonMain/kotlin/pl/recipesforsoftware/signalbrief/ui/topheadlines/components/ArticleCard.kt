package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefShapes
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings

/**
 * Feed card shared between Android and iOS.
 *
 * Editorial hierarchy: source badge on the first line, then the headline
 * (two lines), then the excerpt (two lines), then a fixed-size thumbnail on
 * the right when the article carries an image. Optional fields are dropped
 * rather than filled with placeholders, and [onClick] is a host-provided
 * navigation hook (for example opening [Article.url] in an external browser).
 *
 * When [onBookmarkClick] is provided the card renders a bookmark toggle in the
 * bottom-right corner. The bookmark action is a separate touch target from the
 * card itself so tapping bookmark does not also open the article.
 *
 * The card is a calm surface with a hairline border and no elevation, so it
 * stays dense and readable in both light and dark modes.
 */
@Composable
fun ArticleCard(
    article: Article,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isSaved: Boolean = false,
    onBookmarkClick: (() -> Unit)? = null,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        shape = SignalBriefShapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ArticleCardContent(article = article)
            if (onBookmarkClick != null) {
                BookmarkAction(
                    isSaved = isSaved,
                    onClick = onBookmarkClick,
                    modifier =
                        Modifier
                            .align(Alignment.End)
                            .padding(end = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArticleCardContent(article: Article) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            article.source?.name?.let { sourceName ->
                ArticleHeader(sourceName = sourceName)
            }
            Text(
                text = article.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            article.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val imageUrl = article.imageUrl
        if (imageUrl != null) {
            SignalBriefArticleThumbnail(
                imageReference = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(88.dp, 72.dp)
                        .clip(SignalBriefShapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
    }
}

@Composable
private fun BookmarkAction(
    isSaved: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier.semantics {
                role = Role.Button
            },
    ) {
        Icon(
            imageVector =
                if (isSaved) {
                    BookmarkIcons.Filled
                } else {
                    BookmarkIcons.Outlined
                },
            contentDescription =
                if (isSaved) {
                    TopHeadlinesStrings.BOOKMARK_REMOVE
                } else {
                    TopHeadlinesStrings.BOOKMARK_SAVE
                },
            tint =
                if (isSaved) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

/**
 * Source badge line shared between the feed card and Article Details: source
 * initials on a small primary container followed by the publisher name.
 */
@Composable
internal fun ArticleHeader(sourceName: String) {
    val initials = sourceInitials(sourceName)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (initials != null) {
            Box(
                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = sourceName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
