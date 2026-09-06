package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class FakeTopics : TopicMonitoringRepository {
    val topics = MutableStateFlow<List<MonitoredTopic>>(emptyList())
    var createFailure: Throwable? = null
    var updateFailure: Throwable? = null
    var deleteFailure: Throwable? = null
    var createCalls = 0
    var updateCalls = 0
    var deleteCalls = 0
    var createGate: CompletableDeferred<Unit>? = null
    var updateGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null

    override fun observeTopics() = topics

    override suspend fun createTopic(query: String): Result<MonitoredTopic> {
        createCalls++
        createGate?.await()
        createFailure?.let { return Result.failure(it) }
        return Result.success(MonitoredTopic("new", query.trim())).also { topics.value += it.getOrThrow() }
    }

    override suspend fun updateTopic(
        id: String,
        query: String,
    ): Result<MonitoredTopic> {
        updateCalls++
        updateGate?.await()
        updateFailure?.let { return Result.failure(it) }
        val updated = MonitoredTopic(id, query.trim())
        topics.value = topics.value.map { if (it.id == id) updated else it }
        return Result.success(updated)
    }

    override suspend fun deleteTopic(id: String): Result<Unit> {
        deleteCalls++
        deleteGate?.await()
        deleteFailure?.let { return Result.failure(it) }
        topics.value = topics.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TopicMonitoringPresenterTest {
    private fun presenter(repo: FakeTopics) = TopicMonitoringPresenter(repo, StandardTestDispatcher())

    @Test fun `observes initial and external topics`() =
        runTest {
            val repo = FakeTopics()
            val presenter = TopicMonitoringPresenter(repo, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            assertEquals(emptyList(), presenter.uiState.value.topics)
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            advanceUntilIdle()
            assertEquals(
                "Kotlin",
                presenter.uiState.value.topics
                    .single()
                    .query,
            )
        }

    @Test fun `create success and failures retain appropriate editor state`() =
        runTest {
            val repo = FakeTopics()
            val presenter = TopicMonitoringPresenter(repo, StandardTestDispatcher(testScheduler))
            presenter.openCreateEditor()
            presenter.updateEditorQuery("Kotlin")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.editor)
            assertEquals(false, presenter.uiState.value.isCreating)
            presenter.openCreateEditor()
            repo.createFailure = TopicMonitoringFailure.InvalidQuery
            presenter.confirmEditor()
            advanceUntilIdle()
            assertIs<TopicEditor.Create>(presenter.uiState.value.editor)
            assertEquals(TopicMonitoringUiError.InvalidQuery, presenter.uiState.value.error)
            repo.createFailure = TopicMonitoringFailure.DuplicateQuery
            presenter.confirmEditor()
            advanceUntilIdle()
            assertEquals(TopicMonitoringUiError.DuplicateQuery, presenter.uiState.value.error)
        }

    @Test fun `rename supports success duplicate and own normalized query`() =
        runTest {
            val repo = FakeTopics()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val presenter = TopicMonitoringPresenter(repo, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            presenter.openRenameEditor(topic)
            assertEquals("Kotlin", (presenter.uiState.value.editor as TopicEditor.Rename).query)
            presenter.updateEditorQuery(" kotlin ")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.editor)
            presenter.openRenameEditor(topic)
            repo.updateFailure = TopicMonitoringFailure.DuplicateQuery
            presenter.confirmEditor()
            advanceUntilIdle()
            assertIs<TopicEditor.Rename>(presenter.uiState.value.editor)
            assertEquals(TopicMonitoringUiError.DuplicateQuery, presenter.uiState.value.error)
        }

    @Test fun `delete confirmation cancellation success and not found are handled`() =
        runTest {
            val repo = FakeTopics()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val presenter = TopicMonitoringPresenter(repo, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            presenter.openDeleteConfirmation(topic)
            presenter.dismissDeleteConfirmation()
            assertNull(presenter.uiState.value.pendingDelete)
            presenter.openDeleteConfirmation(topic)
            presenter.confirmDelete()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.pendingDelete)
            presenter.openDeleteConfirmation(topic)
            repo.deleteFailure = TopicMonitoringFailure.NotFound
            presenter.confirmDelete()
            advanceUntilIdle()
            assertEquals(TopicMonitoringUiError.NotFound, presenter.uiState.value.error)
        }

    @Test fun `duplicate in flight create update and delete submits are guarded`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repo = FakeTopics()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val presenter = TopicMonitoringPresenter(repo, dispatcher)
            advanceUntilIdle()

            repo.createGate = CompletableDeferred()
            presenter.openCreateEditor()
            presenter.confirmEditor()
            presenter.confirmEditor()
            runCurrent()
            assertEquals(1, repo.createCalls)
            repo.createGate?.complete(Unit)
            advanceUntilIdle()

            repo.updateGate = CompletableDeferred()
            presenter.openRenameEditor(topic)
            presenter.confirmEditor()
            presenter.confirmEditor()
            runCurrent()
            assertEquals(1, repo.updateCalls)
            repo.updateGate?.complete(Unit)
            advanceUntilIdle()

            repo.deleteGate = CompletableDeferred()
            presenter.openDeleteConfirmation(topic)
            presenter.confirmDelete()
            presenter.confirmDelete()
            runCurrent()
            assertEquals(1, repo.deleteCalls)
            repo.deleteGate?.complete(Unit)
            advanceUntilIdle()
            assertNull(presenter.uiState.value.pendingDelete)
            assertEquals(emptySet(), presenter.uiState.value.mutatingTopicIds)
        }

    @Test fun `failure can be dismissed and retried`() =
        runTest {
            val repo = FakeTopics()
            val presenter = TopicMonitoringPresenter(repo, StandardTestDispatcher(testScheduler))
            presenter.openCreateEditor()
            repo.createFailure = TopicMonitoringFailure.InvalidQuery
            presenter.confirmEditor()
            advanceUntilIdle()
            presenter.dismissError()
            assertNull(presenter.uiState.value.error)
            presenter.confirmEditor()
            advanceUntilIdle()
            assertEquals(2, repo.createCalls)
            assertEquals(TopicMonitoringUiError.InvalidQuery, presenter.uiState.value.error)
        }
}
