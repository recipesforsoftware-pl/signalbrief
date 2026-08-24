package pl.recipesforsoftware.signalbrief.ui.app

/**
 * Top-level destinations in the SignalBrief app shell.
 *
 * Navigation state lives here, in the shared app shell, rather than in any
 * domain model or feature presenter. The shell owns the current [AppDestination]
 * and renders the corresponding content.
 */
internal enum class AppDestination {
    Headlines,
    DailyBrief,
    Saved,
}
