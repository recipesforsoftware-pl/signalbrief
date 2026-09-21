package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

/**
 * Centralized user-facing strings of the Topic Matches screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth.
 */
object TopicMatchesStrings {
    const val TITLE: String = "Topic Matches"
    const val BACK: String = "Back"

    const val NO_LOCAL_ARTICLES_TITLE: String = "No downloaded headlines"
    const val NO_LOCAL_ARTICLES_SUBTITLE: String = "Visit Top Headlines to download the latest news."

    const val NO_MATCHES_TITLE: String = "No matching articles"
    const val NO_MATCHES_SUBTITLE: String = "This topic has no matches in the downloaded headlines."
}
