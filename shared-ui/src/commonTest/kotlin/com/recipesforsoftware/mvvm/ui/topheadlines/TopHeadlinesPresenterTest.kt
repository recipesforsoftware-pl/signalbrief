package com.recipesforsoftware.mvvm.ui.topheadlines

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.FeedSource
import com.recipesforsoftware.mvvm.domain.model.Source
import com.recipesforsoftware.mvvm.domain.model.TopHeadlinesFeed
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Controllable [NewsRepository] fake.
 *
 * Each [getTopHeadlines] call parks on its own gate until [releaseCall] lets it
 * proceed, so tests can orchestrate overlapping requests. All calls run on the
 * single-threaded test scheduler, so the mutable [gates]/[callCount] state
 * needs no synchronization. Cancellation is rethrown (never swallowed) and
 * recorded so tests can assert structured-concurrency behaviour.
 */
private class FakeNewsRepository : NewsRepository {
    private val gates = mutableListOf<CompletableDeferred<Unit>>()

    var nextResult: Result<TopHeadlinesFeed> =
        Result.success(TopHeadlinesFeed(emptyList(), FeedSource.NETWORK))
    var callCount: Int = 0
    var lastCountry: String? = null
    var lastCancelled: Boolean = false

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        val gate = CompletableDeferred<Unit>()
        gates.add(gate)
        callCount++
        lastCountry = country
        try {
            gate.await()
        } catch (e: CancellationException) {
            lastCancelled = true
            throw e
        }
        return nextResult
    }

    /** Lets the N-th (0-based) pending call return [nextResult]. */
    fun releaseCall(index: Int) {
        gates[index].complete(Unit)
    }
}

private val source = Source(id = "test", name = "Test News")

private fun article(id: Int): Article =
    Article(
        title = "Headline $id",
        description = "Description $id",
        url = "https://example.com/$id",
        imageUrl = null,
        source = source,
    )

private fun feed(
    articles: List<Article> = emptyList(),
    source: FeedSource = FeedSource.NETWORK,
): Result<TopHeadlinesFeed> = Result.success(TopHeadlinesFeed(articles, source))

@OptIn(ExperimentalCoroutinesApi::class)
private fun createPresenter(
    repository: NewsRepository,
    scope: TestScope,
    country: String = DEFAULT_NEWS_COUNTRY,
): TopHeadlinesPresenter =
    TopHeadlinesPresenter(
        repository = repository,
        country = country,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TopHeadlinesPresenterTest {
    @Test
    fun `loading is shown while the initial fetch is in flight`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)

            assertEquals(TopHeadlinesUiState.Loading, presenter.uiState.value)
            advanceUntilIdle()
            assertEquals(1, repository.callCount)
            assertEquals(TopHeadlinesUiState.Loading, presenter.uiState.value)

            repository.nextResult = feed(listOf(article(1)))
            repository.releaseCall(0)
            advanceUntilIdle()

            assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
        }

    @Test
    fun `success exposes the loaded articles and the configured country`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this, country = "de")
            advanceUntilIdle()

            repository.nextResult = feed(listOf(article(1), article(2)))
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals("de", repository.lastCountry)
            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertEquals(listOf(article(1), article(2)), state.articles)
            assertEquals(FeedSource.NETWORK, state.source)
        }

    @Test
    fun `cached feed exposes the cache provenance in the success state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = feed(listOf(article(1)), source = FeedSource.CACHE)
            repository.releaseCall(0)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertEquals(listOf(article(1)), state.articles)
            assertEquals(FeedSource.CACHE, state.source)
        }

    @Test
    fun `empty feed maps to the empty state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = feed(emptyList())
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals(TopHeadlinesUiState.Empty, presenter.uiState.value)
        }

    @Test
    fun `network failure maps to the network error state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = Result.failure(NewsFailure.Network)
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals(
                TopHeadlinesUiState.Error(TopHeadlinesError.Network),
                presenter.uiState.value,
            )
        }

    @Test
    fun `invalid data failure maps to the invalid data error state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = Result.failure(NewsFailure.InvalidData)
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals(
                TopHeadlinesUiState.Error(TopHeadlinesError.InvalidData),
                presenter.uiState.value,
            )
        }

    @Test
    fun `unknown typed failure maps to the unknown error state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = Result.failure(NewsFailure.Unknown(IllegalStateException("boom")))
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals(
                TopHeadlinesUiState.Error(TopHeadlinesError.Unknown),
                presenter.uiState.value,
            )
        }

    @Test
    fun `unclassified failure maps to the unknown error state`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = Result.failure(IllegalStateException("boom"))
            repository.releaseCall(0)
            advanceUntilIdle()

            assertEquals(
                TopHeadlinesUiState.Error(TopHeadlinesError.Unknown),
                presenter.uiState.value,
            )
        }

    @Test
    fun `retry after a failure loads fresh data`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)
            advanceUntilIdle()

            repository.nextResult = Result.failure(NewsFailure.Network)
            repository.releaseCall(0)
            advanceUntilIdle()
            assertEquals(TopHeadlinesUiState.Error(TopHeadlinesError.Network), presenter.uiState.value)

            presenter.refresh()
            advanceUntilIdle()
            repository.nextResult = feed(listOf(article(1)))
            repository.releaseCall(1)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertEquals(listOf(article(1)), state.articles)
        }

    @Test
    fun `dispose cancels the in-flight repository call`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)

            advanceUntilIdle()
            assertEquals(1, repository.callCount)

            presenter.dispose()
            advanceUntilIdle()

            assertTrue(repository.lastCancelled, "In-flight call was not cancelled by dispose")
        }

    @Test
    fun `refresh after dispose is ignored`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)

            presenter.dispose()
            presenter.refresh()
            advanceUntilIdle()

            assertEquals(0, repository.callCount, "No call should be started once the scope is cancelled")
        }

    @Test
    fun `stale response does not overwrite a newer one`() =
        runTest {
            val repository = FakeNewsRepository()
            val presenter = createPresenter(repository, this)

            advanceUntilIdle()
            repository.nextResult = feed(listOf(article(1)))
            repository.releaseCall(0)
            advanceUntilIdle()
            assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)

            presenter.refresh()
            advanceUntilIdle()
            assertEquals(2, repository.callCount)

            presenter.refresh()
            advanceUntilIdle()
            assertEquals(3, repository.callCount)

            repository.nextResult = feed(listOf(article(3)))
            repository.releaseCall(2)
            advanceUntilIdle()
            assertEquals(listOf(article(3)), assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value).articles)

            repository.nextResult = Result.failure(NewsFailure.Network)
            repository.releaseCall(1)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertEquals(listOf(article(3)), state.articles, "Stale response must not replace newer data")
        }
}
