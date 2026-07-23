package com.recipesforsoftware.mvvm

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.recipesforsoftware.mvvm.data.model.Article
import com.recipesforsoftware.mvvm.data.model.Source
import com.recipesforsoftware.mvvm.data.repository.TopHeadlineRepository
import com.recipesforsoftware.mvvm.ui.base.UiState
import com.recipesforsoftware.mvvm.ui.topheadline.TopHeadlineViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TopHeadlineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TopHeadlineRepository
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
    fun `initial state is Loading`() = runTest {
        // Given
        coEvery { repository.getTopHeadlines(any()) } returns Result.success(emptyList())

        // When
        viewModel = TopHeadlineViewModel(repository)

        // Then - initial state should be Loading before coroutine completes
        assertTrue(viewModel.uiState.value is UiState.Loading)
    }

    @Test
    fun `fetchTopHeadlines success updates state to Success`() = runTest {
        // Given
        val articles = createFakeArticles(3)
        coEvery { repository.getTopHeadlines(any()) } returns Result.success(articles)

        // When
        viewModel = TopHeadlineViewModel(repository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.size)
    }

    @Test
    fun `fetchTopHeadlines failure updates state to Error`() = runTest {
        // Given
        coEvery { repository.getTopHeadlines(any()) } returns Result.failure(
            RuntimeException("Network error")
        )

        // When
        viewModel = TopHeadlineViewModel(repository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("Network error", (state as UiState.Error).message)
    }

    @Test
    fun `fetchTopHeadlines failure with null message shows default error`() = runTest {
        // Given
        coEvery { repository.getTopHeadlines(any()) } returns Result.failure(RuntimeException())

        // When
        viewModel = TopHeadlineViewModel(repository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals("An unexpected error occurred", (state as UiState.Error).message)
    }

    @Test
    fun `fetchTopHeadlines with empty list updates state to Success with empty list`() = runTest {
        // Given
        coEvery { repository.getTopHeadlines(any()) } returns Result.success(emptyList())

        // When
        viewModel = TopHeadlineViewModel(repository)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(0, (state as UiState.Success).data.size)
    }

    @Test
    fun `fetchTopHeadlines called with correct country`() = runTest {
        // Given
        coEvery { repository.getTopHeadlines("us") } returns Result.success(emptyList())

        // When
        viewModel = TopHeadlineViewModel(repository)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { repository.getTopHeadlines("us") }
    }

    private fun createFakeArticles(count: Int): List<Article> {
        return (1..count).map { index ->
            Article(
                title = "Article $index",
                description = "Description for article $index",
                url = "https://example.com/article/$index",
                imageUrl = "https://example.com/image/$index.jpg",
                source = Source(id = "source-$index", name = "Source $index")
            )
        }
    }
}
