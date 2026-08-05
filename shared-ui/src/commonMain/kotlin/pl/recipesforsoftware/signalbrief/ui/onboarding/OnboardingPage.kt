package pl.recipesforsoftware.signalbrief.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SignalBriefPrimaryButton
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefShapes
import pl.recipesforsoftware.signalbrief.ui.designsystem.tokens.SignalBriefSpacing

private val visualMaxHeight = 320.dp
private val visualMinHeight = 180.dp
private val touchTargetMinHeight = 48.dp
private const val TEXT_ALPHA_PRIMARY = 0.8f
private const val TEXT_ALPHA_SECONDARY = 0.7f
private const val VISUAL_FILL_WIDTH_RATIO = 0.85f

/**
 * One page of the two-page onboarding flow.
 *
 * Centres content on compact and wider screens, supports scrolling and larger
 * font scales, and exposes a clear primary CTA plus a secondary text action.
 */
@Composable
internal fun OnboardingPage(
    pageIndex: Int,
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
                .padding(horizontal = SignalBriefSpacing.pageHorizontal)
                .padding(bottom = SignalBriefSpacing.l)
                .semantics { contentDescription = pageAccessibilityLabel },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        PageContent(
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
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = SignalBriefSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(VISUAL_FILL_WIDTH_RATIO)
                    .heightIn(min = visualMinHeight, max = visualMaxHeight),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingVisual()
        }

        Spacer(modifier = Modifier.height(SignalBriefSpacing.xl))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = TEXT_ALPHA_PRIMARY),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = TEXT_ALPHA_SECONDARY),
                )
            }
        }
    }
}
