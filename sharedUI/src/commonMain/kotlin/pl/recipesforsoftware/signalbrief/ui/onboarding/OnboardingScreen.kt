package pl.recipesforsoftware.signalbrief.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_back
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_continue
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_page_1_body
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_page_1_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_page_2_body
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_page_2_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_skip
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.onboarding_start_reading
import pl.recipesforsoftware.signalbrief.ui.designsystem.components.SigbyVariant

/**
 * Two-page onboarding flow shared between Android and iOS.
 *
 * Displays the Sigby mascot, an editorial title and body, a page indicator,
 * and a primary CTA. Page 1 uses [SigbyVariant.Full] and offers "Continue"
 * and "Skip"; page 2 uses [SigbyVariant.Compact] and offers "Start reading"
 * and "Back". Both [onSkip] and [onComplete] finish the flow and trigger the
 * host's completion callback.
 *
 * @param pageIndex Zero-based page index (0 or 1).
 * @param onContinue Called when the user presses "Continue" on page 1.
 * @param onBack Called when the user presses "Back" on page 2.
 * @param onSkip Called when the user presses "Skip" on page 1.
 * @param onComplete Called when the user presses "Start reading" on page 2.
 */
@Composable
fun OnboardingScreen(
    pageIndex: Int,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = pageIndex,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                slideInHorizontally { width -> direction * width } togetherWith
                    slideOutHorizontally { width -> -direction * width }
            },
            label = "OnboardingPageTransition",
        ) { targetPage ->
            when (targetPage) {
                0 -> {
                    OnboardingPage(
                        pageIndex = 0,
                        sigbyVariant = SigbyVariant.Full,
                        title = stringResource(Res.string.onboarding_page_1_title),
                        body = stringResource(Res.string.onboarding_page_1_body),
                        primaryActionLabel = stringResource(Res.string.onboarding_continue),
                        onPrimaryAction = onContinue,
                        secondaryActionLabel = stringResource(Res.string.onboarding_skip),
                        onSecondaryAction = onSkip,
                    )
                }

                1 -> {
                    OnboardingPage(
                        pageIndex = 1,
                        sigbyVariant = SigbyVariant.Compact,
                        title = stringResource(Res.string.onboarding_page_2_title),
                        body = stringResource(Res.string.onboarding_page_2_body),
                        primaryActionLabel = stringResource(Res.string.onboarding_start_reading),
                        onPrimaryAction = onComplete,
                        secondaryActionLabel = stringResource(Res.string.onboarding_back),
                        onSecondaryAction = onBack,
                    )
                }

                else -> {
                    error("Invalid onboarding page index: $targetPage")
                }
            }
        }
    }
}
