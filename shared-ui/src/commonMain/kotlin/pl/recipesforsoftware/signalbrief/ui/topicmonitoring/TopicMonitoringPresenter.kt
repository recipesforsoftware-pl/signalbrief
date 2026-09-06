package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository

/** Framework-independent state holder for monitored topic management. */
class TopicMonitoringPresenter(
    private val repository: TopicMonitoringRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow(TopicMonitoringUiState())
    val uiState: StateFlow<TopicMonitoringUiState> = _uiState.asStateFlow()

    init {
        scope.launch { repository.observeTopics().collect { update { copy(topics = it) } } }
    }

    fun openCreateEditor() = update { copy(editor = TopicEditor.Create(), error = null) }

    fun openRenameEditor(topic: MonitoredTopic) = update { copy(editor = TopicEditor.Rename(topic), error = null) }

    fun updateEditorQuery(query: String) =
        update {
            copy(
                editor =
                    when (val current = editor) {
                        is TopicEditor.Create -> current.copy(query = query)
                        is TopicEditor.Rename -> current.copy(query = query)
                        null -> null
                    },
            )
        }

    fun dismissEditor() = update { copy(editor = null, error = null) }

    fun confirmEditor() {
        val editor = _uiState.value.editor ?: return
        val topicId = (editor as? TopicEditor.Rename)?.topic?.id
        val isAlreadyMutating =
            if (topicId == null) {
                _uiState.value.isCreating
            } else {
                topicId in _uiState.value.mutatingTopicIds
            }
        if (isAlreadyMutating) return
        update {
            if (topicId == null) {
                copy(isCreating = true, error = null)
            } else {
                copy(mutatingTopicIds = mutatingTopicIds + topicId, error = null)
            }
        }
        scope.launch {
            val result =
                when (editor) {
                    is TopicEditor.Create -> repository.createTopic(editor.query)
                    is TopicEditor.Rename -> repository.updateTopic(editor.topic.id, editor.query)
                }
            result.fold(
                onSuccess = {
                    update {
                        if (topicId ==
                            null
                        ) {
                            copy(editor = null, isCreating = false)
                        } else {
                            copy(editor = null, mutatingTopicIds = mutatingTopicIds - topicId)
                        }
                    }
                },
                onFailure = { failure ->
                    update {
                        if (topicId ==
                            null
                        ) {
                            copy(isCreating = false, error = failure.toUiError())
                        } else {
                            copy(mutatingTopicIds = mutatingTopicIds - topicId, error = failure.toUiError())
                        }
                    }
                },
            )
        }
    }

    fun openDeleteConfirmation(topic: MonitoredTopic) = update { copy(pendingDelete = topic, error = null) }

    fun dismissDeleteConfirmation() = update { copy(pendingDelete = null) }

    fun confirmDelete() {
        val topic = _uiState.value.pendingDelete ?: return
        if (topic.id in _uiState.value.mutatingTopicIds) return
        update { copy(mutatingTopicIds = mutatingTopicIds + topic.id, error = null) }
        scope.launch {
            repository.deleteTopic(topic.id).fold(
                onSuccess = { update { copy(pendingDelete = null, mutatingTopicIds = mutatingTopicIds - topic.id) } },
                onFailure = { failure ->
                    update {
                        copy(
                            pendingDelete = null,
                            mutatingTopicIds = mutatingTopicIds - topic.id,
                            error = failure.toUiError(),
                        )
                    }
                },
            )
        }
    }

    fun dismissError() = update { copy(error = null) }

    fun dispose() = scope.cancel()

    private fun update(transform: TopicMonitoringUiState.() -> TopicMonitoringUiState) {
        _uiState.value = _uiState.value.transform()
    }
}

private fun Throwable.toUiError() =
    when (this) {
        TopicMonitoringFailure.InvalidQuery -> TopicMonitoringUiError.InvalidQuery
        TopicMonitoringFailure.DuplicateQuery -> TopicMonitoringUiError.DuplicateQuery
        TopicMonitoringFailure.NotFound -> TopicMonitoringUiError.NotFound
        else -> TopicMonitoringUiError.Unknown
    }
