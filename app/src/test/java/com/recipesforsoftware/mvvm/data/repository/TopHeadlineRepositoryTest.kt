package com.recipesforsoftware.mvvm.data.repository

import com.google.gson.JsonParseException
import com.recipesforsoftware.mvvm.data.api.NetworkService
import com.recipesforsoftware.mvvm.data.remote.dto.ArticleDto
import com.recipesforsoftware.mvvm.data.remote.dto.SourceDto
import com.recipesforsoftware.mvvm.data.remote.dto.TopHeadlinesResponseDto
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class TopHeadlineRepositoryTest {
    private lateinit var networkService: NetworkService
    private lateinit var repository: TopHeadlineRepository

    @Before
    fun setUp() {
        networkService = mockk()
        repository = TopHeadlineRepository(networkService)
    }

    @Test
    fun `getTopHeadlines returns mapped articles on success`() =
        runTest {
            // Given
            val response =
                TopHeadlinesResponseDto(
                    status = "ok",
                    totalResults = 1,
                    articles =
                        listOf(
                            createArticleDto(
                                title = "Test Title",
                                url = "https://test.com",
                                imageUrl = "https://test.com/image.jpg",
                                source = SourceDto(id = "test-source", name = "Test Source"),
                            ),
                        ),
                )
            coEvery { networkService.getTopHeadlines("us") } returns response

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isSuccess)
            val article = result.getOrNull()?.single()
            assertEquals("Test Title", article?.title)
            assertEquals("https://test.com", article?.url)
            assertEquals("https://test.com/image.jpg", article?.imageUrl)
            assertEquals("Test Source", article?.source?.name)
            coVerify(exactly = 1) { networkService.getTopHeadlines("us") }
        }

    @Test
    fun `getTopHeadlines returns empty list when response has no articles`() =
        runTest {
            // Given
            val response =
                TopHeadlinesResponseDto(
                    status = "ok",
                    totalResults = 0,
                    articles = emptyList(),
                )
            coEvery { networkService.getTopHeadlines("us") } returns response

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `getTopHeadlines returns empty list when articles is null`() =
        runTest {
            // Given
            val response =
                TopHeadlinesResponseDto(
                    status = "ok",
                    totalResults = 0,
                    articles = null,
                )
            coEvery { networkService.getTopHeadlines("us") } returns response

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull()?.size)
        }

    @Test
    fun `getTopHeadlines drops articles without a usable url`() =
        runTest {
            // Given
            val response =
                TopHeadlinesResponseDto(
                    status = "ok",
                    totalResults = 2,
                    articles =
                        listOf(
                            createArticleDto(title = "No url", url = null),
                            createArticleDto(title = "Blank url", url = "  "),
                        ),
                )
            coEvery { networkService.getTopHeadlines("us") } returns response

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
        }

    @Test
    fun `getTopHeadlines maps HttpException to Network failure`() =
        runTest {
            // Given
            val errorResponse =
                Response.error<Any>(
                    500,
                    "error".toResponseBody("application/json".toMediaType()),
                )
            coEvery { networkService.getTopHeadlines("us") } throws HttpException(errorResponse)

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isFailure)
            assertEquals(NewsFailure.Network, result.exceptionOrNull())
        }

    @Test
    fun `getTopHeadlines maps IOException to Network failure`() =
        runTest {
            // Given
            coEvery { networkService.getTopHeadlines("us") } throws IOException("connection reset")

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isFailure)
            assertEquals(NewsFailure.Network, result.exceptionOrNull())
        }

    @Test
    fun `getTopHeadlines maps malformed json to InvalidData failure`() =
        runTest {
            // Given
            coEvery { networkService.getTopHeadlines("us") } throws JsonParseException("malformed json")

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isFailure)
            assertEquals(NewsFailure.InvalidData, result.exceptionOrNull())
        }

    @Test
    fun `getTopHeadlines maps unexpected exception to Unknown failure with cause`() =
        runTest {
            // Given
            coEvery { networkService.getTopHeadlines("us") } throws IllegalStateException("boom")

            // When
            val result = repository.getTopHeadlines("us")

            // Then
            assertTrue(result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue(failure is NewsFailure.Unknown)
            assertEquals("boom", (failure as NewsFailure.Unknown).cause.message)
        }

    @Test
    fun `getTopHeadlines rethrows cancellation instead of reporting a failure`() =
        runTest {
            // Given
            coEvery { networkService.getTopHeadlines("us") } throws CancellationException("cancelled")

            // When
            var thrown: Throwable? = null
            try {
                repository.getTopHeadlines("us")
            } catch (e: CancellationException) {
                thrown = e
            }

            // Then - cancellation propagates and is never wrapped as a failure
            assertTrue(thrown is CancellationException)
        }

    private fun createArticleDto(
        title: String = "Article",
        url: String? = null,
        imageUrl: String? = null,
        source: SourceDto? = null,
    ): ArticleDto =
        ArticleDto(
            title = title,
            description = "Description",
            url = url,
            imageUrl = imageUrl,
            source = source,
        )
}
