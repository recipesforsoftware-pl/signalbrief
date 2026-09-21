package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

object TopicMonitoringStrings {
    const val TITLE = "Monitored topics"
    const val BACK = "Back"
    const val CREATE_TOPIC = "Create monitored topic"
    const val NEW_TOPIC = "New monitored topic"
    const val RENAME_TOPIC = "Rename monitored topic"
    const val TOPIC_QUERY = "Topic"
    const val OPTIONS = "Topic options"
    const val RENAME = "Rename"
    const val DELETE = "Delete"
    const val CANCEL = "Cancel"
    const val CREATE = "Create"
    const val SAVE = "Save"
    const val EMPTY_TITLE = "No monitored topics"
    const val EMPTY_DESCRIPTION = "Add a topic to keep track of stories you care about."
    const val DELETE_TITLE = "Delete monitored topic?"
    const val DELETE_MESSAGE = "This will permanently remove"
    const val NO_DOWNLOADED_HEADLINES = "No downloaded headlines"

    fun matchSummary(count: Int): String = if (count == 1) "1 match" else "$count matches"

    fun error(error: TopicMonitoringUiError): String =
        when (error) {
            TopicMonitoringUiError.InvalidQuery -> "Enter a topic to monitor."
            TopicMonitoringUiError.DuplicateQuery -> "This topic is already being monitored."
            TopicMonitoringUiError.NotFound -> "This topic is no longer available."
            TopicMonitoringUiError.Unknown -> "Something went wrong. Try again."
        }
}
