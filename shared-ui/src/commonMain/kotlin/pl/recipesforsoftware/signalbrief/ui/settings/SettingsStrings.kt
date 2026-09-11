package pl.recipesforsoftware.signalbrief.ui.settings

/**
 * Centralized user-facing strings of the Settings screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth.
 */
object SettingsStrings {
    const val TOP_BAR_TITLE: String = "Settings"
    const val BACK: String = "Back"

    const val OFFLINE_SECTION: String = "Offline"

    const val DOWNLOADED_HEADLINES: String = "Downloaded headlines"

    const val NO_DOWNLOADED_HEADLINES: String = "No downloaded headlines"

    const val CLEAR_DOWNLOADED_HEADLINES: String = "Clear downloaded headlines"
    const val CLEAR_CONFIRMATION_TITLE: String = "Clear downloaded headlines?"
    const val CLEAR_CONFIRMATION_MESSAGE: String =
        "This removes the downloaded headlines from this device. They become available again only after a refresh."
    const val CANCEL: String = "Cancel"
    const val CLEAR: String = "Clear"

    fun downloadedHeadlinesCount(count: Int): String =
        if (count == 1) {
            "1 headline available offline"
        } else {
            "$count headlines available offline"
        }
}
