package pl.recipesforsoftware.signalbrief.ui.settings

/**
 * Immutable UI state for the Settings screen.
 *
 * [downloadedHeadlineCount] is the number of locally cached headlines
 * currently available (0 when none have been downloaded yet).
 */
data class SettingsUiState(
    val downloadedHeadlineCount: Int = 0,
)
