package com.recipesforsoftware.mvvm.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Extracts the current onboarding page index for [OnboardingPresenterSaver].
 */
internal fun saveOnboardingPageIndex(presenter: OnboardingPresenter): Int = presenter.state.value.pageIndex

/**
 * Creates an [OnboardingPresenter] starting on [pageIndex] for
 * [OnboardingPresenterSaver].
 */
internal fun restoreOnboardingPageIndex(page: Int): OnboardingPresenter = OnboardingPresenter(initialPageIndex = page)

/**
 * Compose [Saver] that persists only the current onboarding page index.
 *
 * The presenter instance itself is never saved; [restore] creates a fresh
 * presenter on the saved page, so a host recreation (for example an Android
 * configuration change) resumes the flow where the user left off.
 */
internal val OnboardingPresenterSaver: Saver<OnboardingPresenter, Int> =
    Saver(
        save = { presenter -> saveOnboardingPageIndex(presenter) },
        restore = { pageIndex -> restoreOnboardingPageIndex(pageIndex) },
    )

/**
 * Remembers an [OnboardingPresenter] whose page index survives host recreation
 * through [rememberSaveable]. Page 1 remains page 1 across recompositions; after
 * a recreation the restored instance starts on the saved page (for example page
 * 2) instead of resetting to page 1.
 */
@Composable
internal fun rememberOnboardingPresenter(): OnboardingPresenter =
    rememberSaveable(saver = OnboardingPresenterSaver) { OnboardingPresenter() }
