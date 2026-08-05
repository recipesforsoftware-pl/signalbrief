package com.recipesforsoftware.mvvm.ui.topheadlines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefShapes
import com.recipesforsoftware.mvvm.ui.designsystem.tokens.SignalBriefSpacing
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesStrings

/**
 * Compact notice shown above the feed when articles were served from the
 * persistent cache because the latest online update was unavailable.
 *
 * Announcement strategy: the whole banner exposes a single merged text as a
 * polite live region and clears its children, so screen readers announce the
 * notice once instead of reading the title and body separately.
 */
@Composable
fun CacheNoticeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(SignalBriefShapes.small)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clearAndSetSemantics {
                    liveRegion = LiveRegionMode.Polite
                    text =
                        AnnotatedString(
                            "${TopHeadlinesStrings.CACHE_NOTICE_TITLE}. ${TopHeadlinesStrings.CACHE_NOTICE_BODY}",
                        )
                },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = TopHeadlinesStrings.CACHE_NOTICE_TITLE,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = TopHeadlinesStrings.CACHE_NOTICE_BODY,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
