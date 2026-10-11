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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingCompletion
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingScreen

/** Shared application shell that selects loading, onboarding, or root content. */
@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    installSignalBriefImageLoader()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = onboardingCompleted == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) { InitializingContent() }
        if (onboardingCompleted == false) {
            var onboardingPageIndex by rememberSaveable { mutableStateOf(0) }
            val completion = remember { OnboardingCompletion(onCompleteOnboarding) }
            OnboardingScreen(
                pageIndex = onboardingPageIndex,
                onContinue = { onboardingPageIndex = 1 },
                onBack = { onboardingPageIndex = 0 },
                onSkip = completion::complete,
                onComplete = completion::complete,
            )
        }
        if (onboardingCompleted == true) {
            content()
        }
    }
}

@Composable
private fun InitializingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
