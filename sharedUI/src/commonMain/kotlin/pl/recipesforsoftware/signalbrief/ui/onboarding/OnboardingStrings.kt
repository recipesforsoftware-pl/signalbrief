package pl.recipesforsoftware.signalbrief.ui.onboarding

/**
 * Centralized user-facing strings of the two-page onboarding flow.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies and gives both hosts a single source of truth.
 */
object OnboardingStrings {
    const val PAGE_1_TITLE: String = "A clearer view of the day"
    const val PAGE_1_BODY: String =
        "Read the latest headlines in a focused experience shared across Android and iOS."

    const val PAGE_2_TITLE: String = "Headlines that stay available"
    const val PAGE_2_BODY: String =
        "SignalBrief keeps the latest successful update ready when the network is unavailable."

    const val CONTINUE: String = "Continue"
    const val SKIP: String = "Skip"
    const val START_READING: String = "Start reading"
    const val BACK: String = "Back"

    const val PAGE_1_ACCESSIBILITY_LABEL: String = "Onboarding page one"
    const val PAGE_2_ACCESSIBILITY_LABEL: String = "Onboarding page two"
}
