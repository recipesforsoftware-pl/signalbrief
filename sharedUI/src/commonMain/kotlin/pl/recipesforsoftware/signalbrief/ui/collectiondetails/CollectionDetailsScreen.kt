package pl.recipesforsoftware.signalbrief.ui.collectiondetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard

/** Stateless shared destination showing durable article snapshots in a collection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailsScreen(
    uiState: CollectionDetailsUiState,
    onArticleClick: (Article) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.collection.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, CollectionDetailsStrings.BACK)
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.articles.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding).padding(SignalBriefSpacing.xxl),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        CollectionDetailsStrings.EMPTY_TITLE,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        CollectionDetailsStrings.EMPTY_DESCRIPTION,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = SignalBriefSpacing.maxContentWidth).padding(padding),
                contentPadding =
                    PaddingValues(
                        horizontal = SignalBriefSpacing.pageHorizontal,
                        vertical = SignalBriefSpacing.m,
                    ),
                verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
            ) {
                items(uiState.articles, key = Article::url) { article ->
                    ArticleCard(article = article, onClick = { onArticleClick(article) })
                }
            }
        }
    }
}
