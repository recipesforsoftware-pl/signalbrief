package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

data class TopicMonitoringUiState(
    val topics: List<MonitoredTopic> = emptyList(),
    val editor: TopicEditor? = null,
    val pendingDelete: MonitoredTopic? = null,
    val mutatingTopicIds: Set<String> = emptySet(),
    val isCreating: Boolean = false,
    val error: TopicMonitoringUiError? = null,
)

sealed interface TopicEditor {
    val query: String

    data class Create(
        override val query: String = "",
    ) : TopicEditor

    data class Rename(
        val topic: MonitoredTopic,
        override val query: String = topic.query,
    ) : TopicEditor
}

enum class TopicMonitoringUiError { InvalidQuery, DuplicateQuery, NotFound, Unknown }
