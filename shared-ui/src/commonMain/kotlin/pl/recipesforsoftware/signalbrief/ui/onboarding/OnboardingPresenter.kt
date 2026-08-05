package pl.recipesforsoftware.signalbrief.ui.onboarding

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Framework-independent state holder for the two-page onboarding flow.
 *
 * Owns only the in-memory page navigation state. It does not persist anything
 * and knows nothing about completion; the host receives the completion outcome
 * through the `SignalBriefApp` callback and persists it with its platform
 * mechanism (DataStore on Android, NSUserDefaults on iOS). The shell saves the
 * current page index with `rememberSaveable`, so a restored instance can start
 * on a saved page (for example page 2 after a host recreation).
 *
 * @param initialPageIndex Page the presenter starts on, typically 0 or a value
 *   restored from the host's saved state.
 */
class OnboardingPresenter(
    initialPageIndex: Int = 0,
) {
    private val _state = MutableStateFlow(OnboardingState(initialPageIndex))
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
}
