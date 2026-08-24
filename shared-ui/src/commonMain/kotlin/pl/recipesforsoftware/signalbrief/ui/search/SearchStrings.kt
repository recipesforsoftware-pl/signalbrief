package pl.recipesforsoftware.signalbrief.ui.search

/**
 * Centralized user-facing strings of the Local Search screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth.
 */
object SearchStrings {
    const val TOP_BAR_TITLE: String = "Search"
    const val BACK: String = "Back"

    const val SEARCH: String = "Search"
    const val SEARCH_HEADLINES: String = "Search downloaded headlines"

    const val IDLE_TITLE: String = "Search downloaded headlines"
    const val IDLE_SUBTITLE: String = "Type a word from a headline, description, or source."

    const val NO_RESULTS_TITLE: String = "No matching articles"
    const val NO_RESULTS_SUBTITLE: String = "Try a different word or check your spelling."

    const val NO_LOCAL_ARTICLES_TITLE: String = "No downloaded headlines yet"
    const val NO_LOCAL_ARTICLES_SUBTITLE: String = "Visit Top Headlines to download the latest news."
}
