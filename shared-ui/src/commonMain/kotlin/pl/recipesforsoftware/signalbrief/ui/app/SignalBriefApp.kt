package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingCompletion
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingScreen
import pl.recipesforsoftware.signalbrief.ui.onboarding.rememberOnboardingPresenter

/**
 * Shared application shell for SignalBrief.
 *
 * Decides between the two-page onboarding flow and the main Top Headlines flow
 * based on [onboardingCompleted]:
 * - `null`  -> the persisted value is still loading; a subtle loading indicator
 *              is shown to avoid an onboarding flash.
 * - `false` -> onboarding is shown.
 * - `true`  -> the host-provided [topHeadlinesContent] is shown.
 *
 * The shell also installs the shared Coil image-loader singleton once for the
 * app composition root. Coil uses the Ktor 3 network fetcher and avoids the
 * previous OkHttp dependency; it does **not** automatically share the exact
 * [io.ktor.client.HttpClient] instance owned by the news repository unless one is
 * explicitly injected.
 *
 * The shell keeps [TopHeadlinesScreen] stateless and avoids a navigation
 * framework for only two destinations. Page navigation state is owned by a
 * small `OnboardingPresenter` whose page index is saved through
 * `rememberSaveable`, so the flow survives host recreation (for example an
 * Android configuration change). Both "Skip" and "Start reading" funnel through
 * an [OnboardingCompletion] guard so [onCompleteOnboarding] fires at most once
 * per shell instance; the host persists the outcome itself.
 */
@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    topHeadlinesContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    installSignalBriefImageLoader()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = onboardingCompleted == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            InitializingContent()
        }

        if (onboardingCompleted == false) {
            val onboardingPresenter = rememberOnboardingPresenter()
            val completion = remember { OnboardingCompletion(onCompleteOnboarding) }

            OnboardingScreen(
                presenter = onboardingPresenter,
                onSkip = completion::complete,
                onComplete = completion::complete,
            )
        }

        if (onboardingCompleted == true) {
            topHeadlinesContent()
        }
    }
}

@Composable
private fun InitializingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
