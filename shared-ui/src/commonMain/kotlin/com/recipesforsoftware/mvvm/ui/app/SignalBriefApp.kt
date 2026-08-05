package com.recipesforsoftware.mvvm.ui.app

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
import com.recipesforsoftware.mvvm.ui.onboarding.OnboardingPresenter
import com.recipesforsoftware.mvvm.ui.onboarding.OnboardingScreen

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
 * The shell keeps [TopHeadlinesScreen] stateless and avoids a navigation
 * framework for only two destinations. Page navigation state is owned by a
 * small [OnboardingPresenter] remembered inside this composable.
 */
@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    topHeadlinesContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = onboardingCompleted == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            InitializingContent()
        }

        if (onboardingCompleted == false) {
            val onboardingPresenter = remember { OnboardingPresenter() }

            OnboardingScreen(
                presenter = onboardingPresenter,
                onSkip = onCompleteOnboarding,
                onComplete = onCompleteOnboarding,
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
