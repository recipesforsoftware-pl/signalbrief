package pl.recipesforsoftware.signalbrief.ui.topheadlines

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class TopHeadlinesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NewsRepository
    private lateinit var viewModel: TopHeadlinesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fakeFeed(
        count: Int = 0,
        source: FeedSource = FeedSource.NETWORK,
    ): Result<TopHeadlinesFeed> = Result.success(TopHeadlinesFeed(createFakeArticles(count), source))

    @Test
    fun `initial state is Loading`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns fakeFeed()

            // When
            viewModel = TopHeadlinesViewModel(repository)

            // Then - initial state should be Loading before the coroutine completes
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Loading)
        }

    @Test
    fun `success updates state to Success`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns fakeFeed(count = 3)

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Success)
            assertEquals(3, (state as TopHeadlinesUiState.Success).articles.size)
            assertEquals(FeedSource.NETWORK, state.source)
        }

    @Test
    fun `cached feed keeps the cache provenance in the success state`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns fakeFeed(count = 1, source = FeedSource.CACHE)

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Success)
            assertEquals(FeedSource.CACHE, (state as TopHeadlinesUiState.Success).source)
        }

    @Test
    fun `empty list updates state to Empty`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns fakeFeed()

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Empty)
        }

    @Test
    fun `network failure updates state to Error with network message`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.failure(NewsFailure.Network)

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Error)
            assertEquals(
                TopHeadlinesStrings.errorBody(TopHeadlinesError.Network),
                (state as TopHeadlinesUiState.Error).errorBody(),
            )
        }

    @Test
    fun `invalid data failure updates state to Error with invalid data message`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.failure(NewsFailure.InvalidData)

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Error)
            assertEquals(
                TopHeadlinesStrings.errorBody(TopHeadlinesError.InvalidData),
                (state as TopHeadlinesUiState.Error).errorBody(),
            )
        }

    @Test
    fun `unknown failure updates state to Error with default message`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns
                Result.failure(NewsFailure.Unknown(IllegalStateException("boom")))

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Error)
            assertEquals(
                TopHeadlinesStrings.errorBody(TopHeadlinesError.Unknown),
                (state as TopHeadlinesUiState.Error).errorBody(),
            )
        }

    @Test
    fun `loads headlines for the configured country`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines("us") } returns fakeFeed()

            // When
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { repository.getTopHeadlines("us") }
        }

    @Test
    fun `refresh after success resets state to Loading before applying new result`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns fakeFeed(count = 1)
            viewModel = TopHeadlinesViewModel(repository)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Success)

            // And the next fetch suspends before completing
            val gate = CompletableDeferred<Unit>()
            coEvery { repository.getTopHeadlines(any()) } coAnswers {
                gate.await()
                Result.failure(NewsFailure.Network)
            }

            // When
            viewModel.refresh()
            testDispatcher.scheduler.runCurrent()

            // Then - loading is shown while the fetch is in flight
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Loading)

            // When - the fetch completes
            gate.complete(Unit)
            advanceUntilIdle()

            // Then - error is applied afterwards
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Error)
        }

    private fun createFakeArticles(count: Int): List<Article> =
        (1..count).map { index ->
            Article(
                title = "Article $index",
                description = "Description for article $index",
                url = "https://example.com/article/$index",
                imageUrl = "https://example.com/image/$index.jpg",
                source = Source(id = "source-$index", name = "Source $index"),
            )
        }
}

private fun TopHeadlinesUiState.Error.errorBody(): String = TopHeadlinesStrings.errorBody(error)
