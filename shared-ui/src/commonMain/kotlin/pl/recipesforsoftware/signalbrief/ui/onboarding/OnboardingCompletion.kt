package pl.recipesforsoftware.signalbrief.ui.onboarding

/**
 * Ensures the host completion callback runs at most once per app-shell instance.
 *
 * Both "Skip" and "Start reading" terminate the onboarding flow. Rapid double
 * taps or recomposition lag must not invoke the host callback twice, so the
 * shell funnels both actions through this guard.
 */
class OnboardingCompletion(
    private val onCompleted: () -> Unit,
) {
    private var hasCompleted = false

    /** Runs [onCompleted] if this instance has not completed yet. */
    fun complete() {
        if (hasCompleted) return
        hasCompleted = true
        onCompleted()
    }
}
