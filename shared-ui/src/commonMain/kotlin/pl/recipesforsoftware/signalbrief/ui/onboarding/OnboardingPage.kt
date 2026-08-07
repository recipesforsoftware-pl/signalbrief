package pl.recipesforsoftware.signalbrief.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.OnboardingPageIndicator
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.Sigby
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SignalBriefPrimaryButton
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefShapes
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

private const val SIGBY_FULL_WIDTH_RATIO = 0.55f
private const val SIGBY_COMPACT_WIDTH_RATIO = 0.42f
private val sigbyFullMaxSize = 200.dp
private val sigbyCompactMaxSize = 150.dp
private val touchTargetMinHeight = 48.dp

/**
 * One page of the two-page onboarding flow.
 *
 * Renders the Sigby mascot, an editorial heading and body, a page indicator,
 * and a clear primary CTA plus a secondary text action. Content is pinned to
 * the top and bottom of the screen when space allows and scrolls only when the
 * available height cannot fit the content (for example with larger font
 * scales). Safe-area insets keep everything clear of system bars on both
 * Android and iOS while the screen-level background stays edge to edge.
 *
 * @param pageIndex Zero-based page index used by the page indicator.
 * @param sigbyVariant Which Sigby artwork to show: [SigbyVariant.Full] on the
 *   opening page and [SigbyVariant.Compact] on the closing page. Sigby is
 *   decorative here, so it is not exposed to accessibility services; all
 *   meaning is carried by [title], [body], and the action labels.
 */
@Composable
internal fun OnboardingPage(
    pageIndex: Int,
    sigbyVariant: SigbyVariant,
    title: String,
    body: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String?,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    pageAccessibilityLabel: String =
        if (pageIndex == 0) {
            OnboardingStrings.PAGE_1_ACCESSIBILITY_LABEL
        } else {
            OnboardingStrings.PAGE_2_ACCESSIBILITY_LABEL
        },
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = SignalBriefSpacing.pageHorizontal)
                .padding(bottom = SignalBriefSpacing.xl)
                .semantics { contentDescription = pageAccessibilityLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        PageContent(
            sigbyVariant = sigbyVariant,
            title = title,
            body = body,
            modifier = Modifier.weight(1f, fill = false),
        )

        ActionArea(
            pageIndex = pageIndex,
            primaryActionLabel = primaryActionLabel,
            onPrimaryAction = onPrimaryAction,
            secondaryActionLabel = secondaryActionLabel,
            onSecondaryAction = onSecondaryAction,
        )
    }
}

@Composable
private fun PageContent(
    sigbyVariant: SigbyVariant,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .widthIn(max = SignalBriefSpacing.maxContentWidth)
                .padding(top = SignalBriefSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SigbyHeader(sigbyVariant = sigbyVariant)

        Spacer(modifier = Modifier.height(SignalBriefSpacing.xl))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { heading() }
                    .padding(horizontal = SignalBriefSpacing.l),
        )

        Spacer(modifier = Modifier.height(SignalBriefSpacing.l))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SigbyHeader(
    sigbyVariant: SigbyVariant,
    modifier: Modifier = Modifier,
) {
    val widthRatio =
        when (sigbyVariant) {
            SigbyVariant.Full -> SIGBY_FULL_WIDTH_RATIO
            SigbyVariant.Compact -> SIGBY_COMPACT_WIDTH_RATIO
        }
    val maxSize =
        when (sigbyVariant) {
            SigbyVariant.Full -> sigbyFullMaxSize
            SigbyVariant.Compact -> sigbyCompactMaxSize
        }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Sigby(
            variant = sigbyVariant,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth(widthRatio)
                    .sizeIn(maxWidth = maxSize, maxHeight = maxSize)
                    .aspectRatio(1f),
        )
    }
}

@Composable
private fun ActionArea(
    pageIndex: Int,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String?,
    onSecondaryAction: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .widthIn(max = SignalBriefSpacing.maxContentWidth)
                .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SignalBriefSpacing.m),
    ) {
        OnboardingPageIndicator(
            pageCount = OnboardingState.PAGE_COUNT,
            currentPage = pageIndex,
        )

        Spacer(modifier = Modifier.height(SignalBriefSpacing.s))

        SignalBriefPrimaryButton(
            text = primaryActionLabel,
            onClick = onPrimaryAction,
        )

        if (secondaryActionLabel != null) {
            TextButton(
                onClick = onSecondaryAction,
                modifier = Modifier.heightIn(min = touchTargetMinHeight),
                shape = SignalBriefShapes.medium,
            ) {
                Text(
                    text = secondaryActionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
