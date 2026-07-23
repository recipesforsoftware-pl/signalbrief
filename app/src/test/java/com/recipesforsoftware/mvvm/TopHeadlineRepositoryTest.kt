package com.recipesforsoftware.mvvm

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import com.recipesforsoftware.mvvm.data.api.NetworkService
import com.recipesforsoftware.mvvm.data.model.Article
import com.recipesforsoftware.mvvm.data.model.Source
import com.recipesforsoftware.mvvm.data.model.TopHeadlinesResponse
import com.recipesforsoftware.mvvm.data.repository.TopHeadlineRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TopHeadlineRepositoryTest {

    private lateinit var networkService: NetworkService
    private lateinit var repository: TopHeadlineRepository

    @Before
    fun setUp() {
        networkService = mockk()
        repository = TopHeadlineRepository(networkService)
    }

    @Test
    fun `getTopHeadlines returns articles on success`() = runTest {
        // Given
        val articles = createFakeArticles(5)
        val response = TopHeadlinesResponse(
            status = "ok",
            totalResults = 5,
            articles = articles
        )
        coEvery { networkService.getTopHeadlines("us") } returns response

        // When
        val result = repository.getTopHeadlines("us")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()?.size)
        coVerify(exactly = 1) { networkService.getTopHeadlines("us") }
    }

    @Test
    fun `getTopHeadlines returns empty list when no articles`() = runTest {
        // Given
        val response = TopHeadlinesResponse(
            status = "ok",
            totalResults = 0,
            articles = emptyList()
        )
        coEvery { networkService.getTopHeadlines("us") } returns response

        // When
        val result = repository.getTopHeadlines("us")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getTopHeadlines returns empty list when articles is null`() = runTest {
        // Given
        val response = TopHeadlinesResponse(
            status = "ok",
            totalResults = 0,
            articles = null
        )
        coEvery { networkService.getTopHeadlines("us") } returns response

        // When
        val result = repository.getTopHeadlines("us")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `getTopHeadlines returns failure on network error`() = runTest {
        // Given
        coEvery { networkService.getTopHeadlines("us") } throws RuntimeException("Network error")

        // When
        val result = repository.getTopHeadlines("us")

        // Then
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getTopHeadlines returns failure on HTTP error`() = runTest {
        // Given
        coEvery { networkService.getTopHeadlines("us") } throws RuntimeException("HTTP 500")

        // When
        val result = repository.getTopHeadlines("us")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getTopHeadlines maps articles correctly`() = runTest {
        // Given
        val article = Article(
            title = "Test Title",
            description = "Test Description",
            url = "https://test.com",
            imageUrl = "https://test.com/image.jpg",
            source = Source(id = "test-source", name = "Test Source")
        )
        val response = TopHeadlinesResponse(
            status = "ok",
            totalResults = 1,
            articles = listOf(article)
        )
        coEvery { networkService.getTopHeadlines("gb") } returns response

        // When
        val result = repository.getTopHeadlines("gb")

        // Then
        assertTrue(result.isSuccess)
        val articles = result.getOrNull()
        assertEquals(1, articles?.size)
        assertEquals("Test Title", articles?.first()?.title)
        assertEquals("Test Source", articles?.first()?.source?.name)
    }

    private fun createFakeArticles(count: Int): List<Article> {
        return (1..count).map { index ->
            Article(
                title = "Article $index",
                description = "Description $index",
                url = "https://example.com/$index",
                imageUrl = "https://example.com/img/$index.jpg",
                source = Source(id = "src-$index", name = "Source $index")
            )
        }
    }
}
