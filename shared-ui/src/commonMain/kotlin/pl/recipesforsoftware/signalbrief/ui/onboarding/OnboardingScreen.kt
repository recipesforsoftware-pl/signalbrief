package pl.recipesforsoftware.signalbrief.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Two-page onboarding flow shared between Android and iOS.
 *
 * Displays an editorial visual, title, body, a page indicator, and a primary CTA.
 * Page 1 offers "Continue" and "Skip"; page 2 offers "Start reading" and
 * "Back". Both [onSkip] and [onComplete] finish the flow and trigger the host's
 * completion callback.
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
    Box(modifier = modifier.fillMaxSize()) {
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
                        title = OnboardingStrings.PAGE_1_TITLE,
                        body = OnboardingStrings.PAGE_1_BODY,
                        primaryActionLabel = OnboardingStrings.CONTINUE,
                        onPrimaryAction = onContinue,
                        secondaryActionLabel = OnboardingStrings.SKIP,
                        onSecondaryAction = onSkip,
                    )
                }

                1 -> {
                    OnboardingPage(
                        pageIndex = 1,
                        title = OnboardingStrings.PAGE_2_TITLE,
                        body = OnboardingStrings.PAGE_2_BODY,
                        primaryActionLabel = OnboardingStrings.START_READING,
                        onPrimaryAction = onComplete,
                        secondaryActionLabel = OnboardingStrings.BACK,
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

/**
 * Stateful version of the onboarding screen driven by an [OnboardingPresenter].
 *
 * Useful for hosts that want to delegate page navigation to the shared presenter.
 */
@Composable
fun OnboardingScreen(
    presenter: OnboardingPresenter,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by presenter.state.collectAsState()

    OnboardingScreen(
        pageIndex = state.pageIndex,
        onContinue = presenter::nextPage,
        onBack = presenter::previousPage,
        onSkip = onSkip,
        onComplete = onComplete,
        modifier = modifier,
    )
}
