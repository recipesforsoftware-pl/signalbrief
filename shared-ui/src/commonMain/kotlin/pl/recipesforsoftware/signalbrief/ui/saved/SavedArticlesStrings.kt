package pl.recipesforsoftware.signalbrief.ui.saved

/**
 * Centralized user-facing strings of the Saved Articles screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth.
 */
object SavedArticlesStrings {
    const val TOP_BAR_TITLE: String = "Saved"

    const val REMOVE: String = "Remove from saved"

    const val EMPTY_TITLE: String = "No saved articles yet"
    const val EMPTY_SUBTITLE: String = "Save stories from Top Headlines to keep them here."
    const val BROWSE_HEADLINES: String = "Browse Headlines"
}
