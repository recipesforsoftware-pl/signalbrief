package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

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

private class FakeMonitoringNewsRepository : NewsRepository {
    private val cachedArticles = MutableStateFlow<List<Article>>(emptyList())

    var getTopHeadlinesCallCount: Int = 0
    var observeFailure: Throwable? = null

    fun seed(articles: List<Article>) {
        cachedArticles.value = articles
    }

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        getTopHeadlinesCallCount++
        error("Topic monitoring must never call the network-first feed path")
    }

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> {
        val failure = observeFailure
        if (failure != null) return flow { throw failure }
        return cachedArticles
    }

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> = Result.success(Unit)
}

private fun testArticle(
    url: String,
    title: String = "Headline for $url",
    description: String = "Description for $url",
    sourceName: String = "Test News",
): Article =
    Article(
        title = title,
        description = description,
        url = url,
        imageUrl = null,
        source = Source(id = null, name = sourceName),
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun createViewModel(
    repo: FakeTopics,
    newsRepo: FakeMonitoringNewsRepository,
): TopicMonitoringViewModel =
    TopicMonitoringViewModel(
        repository = repo,
        newsRepository = newsRepo,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TopicMonitoringViewModelTest {
    @Test fun `observes initial and external topics`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(emptyList(), viewModel.uiState.value.topics)
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            advanceUntilIdle()
            assertEquals(
                "Kotlin",
                viewModel.uiState.value.topics
                    .single()
                    .query,
            )
        }

    @Test fun `create success and failures retain appropriate editor state`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val viewModel = createViewModel(repo, newsRepo)
            viewModel.openCreateEditor()
            viewModel.updateEditorQuery("Kotlin")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.editor)
            assertEquals(false, viewModel.uiState.value.isCreating)
            viewModel.openCreateEditor()
            repo.createFailure = TopicMonitoringFailure.InvalidQuery
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertIs<TopicEditor.Create>(viewModel.uiState.value.editor)
            assertEquals(TopicMonitoringUiError.InvalidQuery, viewModel.uiState.value.error)
            repo.createFailure = TopicMonitoringFailure.DuplicateQuery
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(TopicMonitoringUiError.DuplicateQuery, viewModel.uiState.value.error)
        }

    @Test fun `rename supports success duplicate and own normalized query`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            viewModel.openRenameEditor(topic)
            assertEquals("Kotlin", (viewModel.uiState.value.editor as TopicEditor.Rename).query)
            viewModel.updateEditorQuery(" kotlin ")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.editor)
            viewModel.openRenameEditor(topic)
            repo.updateFailure = TopicMonitoringFailure.DuplicateQuery
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertIs<TopicEditor.Rename>(viewModel.uiState.value.editor)
            assertEquals(TopicMonitoringUiError.DuplicateQuery, viewModel.uiState.value.error)
        }

    @Test fun `delete confirmation cancellation success and not found are handled`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            viewModel.openDeleteConfirmation(topic)
            viewModel.dismissDeleteConfirmation()
            assertNull(viewModel.uiState.value.pendingDelete)
            viewModel.openDeleteConfirmation(topic)
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.pendingDelete)
            viewModel.openDeleteConfirmation(topic)
            repo.deleteFailure = TopicMonitoringFailure.NotFound
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertEquals(TopicMonitoringUiError.NotFound, viewModel.uiState.value.error)
        }

    @Test fun `duplicate in flight create update and delete submits are guarded`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()

            repo.createGate = CompletableDeferred()
            viewModel.openCreateEditor()
            viewModel.confirmEditor()
            viewModel.confirmEditor()
            runCurrent()
            assertEquals(1, repo.createCalls)
            repo.createGate?.complete(Unit)
            advanceUntilIdle()

            repo.updateGate = CompletableDeferred()
            viewModel.openRenameEditor(topic)
            viewModel.confirmEditor()
            viewModel.confirmEditor()
            runCurrent()
            assertEquals(1, repo.updateCalls)
            repo.updateGate?.complete(Unit)
            advanceUntilIdle()

            repo.deleteGate = CompletableDeferred()
            viewModel.openDeleteConfirmation(topic)
            viewModel.confirmDelete()
            viewModel.confirmDelete()
            runCurrent()
            assertEquals(1, repo.deleteCalls)
            repo.deleteGate?.complete(Unit)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.pendingDelete)
            assertEquals(emptySet(), viewModel.uiState.value.mutatingTopicIds)
        }

    @Test fun `failure can be dismissed and retried`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val viewModel = createViewModel(repo, newsRepo)
            viewModel.openCreateEditor()
            repo.createFailure = TopicMonitoringFailure.InvalidQuery
            viewModel.confirmEditor()
            advanceUntilIdle()
            viewModel.dismissError()
            assertNull(viewModel.uiState.value.error)
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(2, repo.createCalls)
            assertEquals(TopicMonitoringUiError.InvalidQuery, viewModel.uiState.value.error)
        }

    @Test fun `match counts reflect cached headlines per topic`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"), MonitoredTopic("2", "Compose"))
            newsRepo.seed(
                listOf(
                    testArticle("https://example.com/1", title = "Kotlin news"),
                    testArticle("https://example.com/2", title = "Compose news"),
                    testArticle("https://example.com/3", title = "Kotlin Multiplatform"),
                    testArticle("https://example.com/4", title = "Swift daily"),
                ),
            )
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertEquals(2, state.matchCountsByTopicId["1"])
            assertEquals(1, state.matchCountsByTopicId["2"])
            assertEquals(true, state.hasLocalArticles)
        }

    @Test fun `source name matches contribute to counts`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Reuters"))
            newsRepo.seed(
                listOf(
                    testArticle("https://example.com/1", title = "Markets today", sourceName = "Reuters"),
                    testArticle("https://example.com/2", title = "Election update", sourceName = "AP News"),
                ),
            )
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.matchCountsByTopicId["1"])
        }

    @Test fun `cached feed updates recompute counts reactively`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            newsRepo.seed(listOf(testArticle("https://example.com/1", title = "Kotlin news")))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.matchCountsByTopicId["1"])
            newsRepo.seed(
                listOf(
                    testArticle("https://example.com/1", title = "Kotlin news"),
                    testArticle("https://example.com/2", title = "Kotlin two"),
                ),
            )
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.matchCountsByTopicId["1"])
        }

    @Test fun `external topic emission recomputes counts`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            newsRepo.seed(listOf(testArticle("https://example.com/1", title = "Kotlin news")))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.matchCountsByTopicId["1"])
            repo.topics.value = listOf(MonitoredTopic("1", "Swift"), MonitoredTopic("2", "Kotlin"))
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.matchCountsByTopicId["1"])
            assertEquals(1, viewModel.uiState.value.matchCountsByTopicId["2"])
        }

    @Test fun `topic rename recomputes match count`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            val topic = MonitoredTopic("1", "Kotlin")
            repo.topics.value = listOf(topic)
            newsRepo.seed(
                listOf(
                    testArticle("https://example.com/1", title = "Kotlin news"),
                    testArticle("https://example.com/2", title = "Kotlin Multiplatform"),
                    testArticle("https://example.com/3", title = "Swift daily"),
                ),
            )
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(2, viewModel.uiState.value.matchCountsByTopicId["1"])
            viewModel.openRenameEditor(topic)
            viewModel.updateEditorQuery("Swift")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.matchCountsByTopicId["1"])
        }

    @Test fun `empty cache reports no local articles`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.hasLocalArticles)
            assertEquals(0, viewModel.uiState.value.matchCountsByTopicId["1"])
        }

    @Test fun `non empty cache with no matching headlines reports zero`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            newsRepo.seed(listOf(testArticle("https://example.com/1", title = "Swift daily")))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.hasLocalArticles)
            assertEquals(0, viewModel.uiState.value.matchCountsByTopicId["1"])
        }

    @Test fun `match counts never call getTopHeadlines`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            newsRepo.seed(listOf(testArticle("https://example.com/1", title = "Kotlin news")))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test fun `cached headline observation failure is treated as empty cache`() =
        runViewModelTest {
            val repo = FakeTopics()
            val newsRepo = FakeMonitoringNewsRepository()
            repo.topics.value = listOf(MonitoredTopic("1", "Kotlin"))
            newsRepo.observeFailure = RuntimeException("cached observation unavailable")
            newsRepo.seed(listOf(testArticle("https://example.com/1", title = "Kotlin news")))
            val viewModel = createViewModel(repo, newsRepo)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.hasLocalArticles)
            assertEquals(0, viewModel.uiState.value.matchCountsByTopicId["1"])
        }
}
