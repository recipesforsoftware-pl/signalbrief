package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.domain.usecase.ArticleQueryMatcher
import pl.recipesforsoftware.signalbrief.ui.topheadlines.DEFAULT_NEWS_COUNTRY

/**
 * Framework-independent state holder for monitored topic management.
 *
 * Combines [TopicMonitoringRepository.observeTopics] with the locally cached
 * top headlines (via [NewsRepository.observeCachedTopHeadlines]) so each topic
 * exposes a reactive match-count summary derived in memory with the shared
 * [ArticleQueryMatcher]. No remote search request is ever issued. A cached
 * observation failure is treated as an empty local cache, consistent with the
 * Local Search convention.
 */
class TopicMonitoringPresenter(
    private val repository: TopicMonitoringRepository,
    private val newsRepository: NewsRepository,
    private val matcher: ArticleQueryMatcher = ArticleQueryMatcher,
    private val country: String = DEFAULT_NEWS_COUNTRY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow(TopicMonitoringUiState())
    val uiState: StateFlow<TopicMonitoringUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                repository.observeTopics(),
                observeCachedArticles(newsRepository, country),
            ) { topics, articles -> topics to articles }
                .collect { (topics, articles) ->
                    update {
                        copy(
                            topics = topics,
                            matchCountsByTopicId = matchCountsFor(topics, articles, matcher),
                            hasLocalArticles = articles.isNotEmpty(),
                        )
                    }
                }
        }
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

private fun observeCachedArticles(
    newsRepository: NewsRepository,
    country: String,
): Flow<List<Article>> =
    newsRepository
        .observeCachedTopHeadlines(country)
        .catch { emit(emptyList()) }

private fun matchCountsFor(
    topics: List<MonitoredTopic>,
    articles: List<Article>,
    matcher: ArticleQueryMatcher,
): Map<String, Int> =
    topics.associate { topic ->
        topic.id to articles.count { matcher.matches(it, topic.query) }
    }

private fun Throwable.toUiError() =
    when (this) {
        TopicMonitoringFailure.InvalidQuery -> TopicMonitoringUiError.InvalidQuery
        TopicMonitoringFailure.DuplicateQuery -> TopicMonitoringUiError.DuplicateQuery
        TopicMonitoringFailure.NotFound -> TopicMonitoringUiError.NotFound
        else -> TopicMonitoringUiError.Unknown
    }
