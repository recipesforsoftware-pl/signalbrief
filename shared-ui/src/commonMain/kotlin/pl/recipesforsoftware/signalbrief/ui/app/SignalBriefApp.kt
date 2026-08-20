package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * Decides between the two-page onboarding flow and the main two-destination
 * app based on [onboardingCompleted]:
 * - `null`  -> the persisted value is still loading; a subtle loading indicator
 *              is shown to avoid an onboarding flash.
 * - `false` -> onboarding is shown.
 * - `true`  -> the host-provided destination content with a two-item bottom
 *              navigation bar (Headlines / Saved) is shown.
 *
 * The shell owns the destination navigation state. No navigation library is
 * needed for exactly two destinations; a small [AppDestination] enum and
 * `rememberSaveable` with an explicit [Saver] is the smallest coherent solution.
 * The selected destination survives host recreation (for example an Android
 * configuration change) and defaults to [AppDestination.Headlines].
 *
 * The shell also installs the shared Coil image-loader singleton once for the
 * app composition root. Both "Skip" and "Start reading" funnel through an
 * [OnboardingCompletion] guard so [onCompleteOnboarding] fires at most once
 * per shell instance; the host persists the outcome itself.
 */
@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    topHeadlinesContent: @Composable (bottomBar: @Composable () -> Unit) -> Unit,
    savedContent: @Composable (bottomBar: @Composable () -> Unit) -> Unit,
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
            var currentDestination by rememberSaveable(stateSaver = AppDestinationSaver) {
                mutableStateOf(AppDestination.Headlines)
            }

            val bottomBar: @Composable () -> Unit = {
                SignalBriefBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { currentDestination = it },
                )
            }

            when (currentDestination) {
                AppDestination.Headlines -> {
                    topHeadlinesContent(bottomBar)
                }

                AppDestination.Saved -> {
                    savedContent(bottomBar)
                }
            }
        }
    }
}

@Composable
private fun SignalBriefBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == AppDestination.Headlines,
            onClick = { onNavigate(AppDestination.Headlines) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Headlines,
                    contentDescription = null,
                )
            },
            label = { Text("Headlines") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
        NavigationBarItem(
            selected = currentDestination == AppDestination.Saved,
            onClick = { onNavigate(AppDestination.Saved) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Saved,
                    contentDescription = null,
                )
            },
            label = { Text("Saved") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
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

/**
 * Compose [Saver] that persists the selected [AppDestination] as its enum name.
 *
 * The enum is not JVM-serializable and must not assume Android-only save/restore
 * semantics; saving the stable name and restoring by lookup keeps the strategy
 * compatible with Compose Multiplatform.
 */
internal val AppDestinationSaver: Saver<AppDestination, String> =
    Saver(
        save = { destination -> destination.name },
        restore = { name -> AppDestination.entries.find { it.name == name } },
    )
