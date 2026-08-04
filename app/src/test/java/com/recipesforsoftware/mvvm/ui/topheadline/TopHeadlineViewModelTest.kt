package com.recipesforsoftware.mvvm.ui.topheadline

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.Source
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesError
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesStrings
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesUiState
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

@OptIn(ExperimentalCoroutinesApi::class)
class TopHeadlineViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NewsRepository
    private lateinit var viewModel: TopHeadlineViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.success(emptyList())

            // When
            viewModel = TopHeadlineViewModel(repository)

            // Then - initial state should be Loading before the coroutine completes
            assertTrue(viewModel.uiState.value is TopHeadlinesUiState.Loading)
        }

    @Test
    fun `success updates state to Success`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.success(createFakeArticles(3))

            // When
            viewModel = TopHeadlineViewModel(repository)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state is TopHeadlinesUiState.Success)
            assertEquals(3, (state as TopHeadlinesUiState.Success).articles.size)
        }

    @Test
    fun `empty list updates state to Empty`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.success(emptyList())

            // When
            viewModel = TopHeadlineViewModel(repository)
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
            viewModel = TopHeadlineViewModel(repository)
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
            viewModel = TopHeadlineViewModel(repository)
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
            viewModel = TopHeadlineViewModel(repository)
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
            coEvery { repository.getTopHeadlines("us") } returns Result.success(emptyList())

            // When
            viewModel = TopHeadlineViewModel(repository)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { repository.getTopHeadlines("us") }
        }

    @Test
    fun `refresh after success resets state to Loading before applying new result`() =
        runTest {
            // Given
            coEvery { repository.getTopHeadlines(any()) } returns Result.success(createFakeArticles(1))
            viewModel = TopHeadlineViewModel(repository)
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
