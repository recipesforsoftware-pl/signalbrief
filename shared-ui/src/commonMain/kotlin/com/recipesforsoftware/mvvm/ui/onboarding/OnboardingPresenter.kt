package com.recipesforsoftware.mvvm.ui.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Framework-independent state holder for the two-page onboarding flow.
 *
 * Owns only the in-memory navigation state (current page and completion). It
 * does not persist anything; the host receives [onComplete] and persists the
 * outcome via its platform mechanism (DataStore on Android, NSUserDefaults on
 * iOS). The same instance survives configuration changes / recompositions.
 */
class OnboardingPresenter {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    /** Advances from page 1 to page 2. Has no effect on the last page. */
    fun nextPage() {
        val current = _state.value
        if (current.pageIndex < OnboardingState.MAX_PAGE_INDEX) {
            _state.value = current.copy(pageIndex = current.pageIndex + 1)
        }
    }

    /** Returns from page 2 to page 1. Has no effect on the first page. */
    fun previousPage() {
        val current = _state.value
        if (current.pageIndex > 0) {
            _state.value = current.copy(pageIndex = current.pageIndex - 1)
        }
    }

    /**
     * Marks onboarding as complete.
     *
     * Should be invoked by the host when the user taps "Skip" or
     * "Start reading". The host is responsible for persisting the completion
     * flag and then routing to the main content.
     */
    fun complete() {
        _state.value = _state.value.copy(isComplete = true)
    }
}
