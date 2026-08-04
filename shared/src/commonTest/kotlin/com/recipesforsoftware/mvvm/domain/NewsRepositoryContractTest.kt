package com.recipesforsoftware.mvvm.domain

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves a plain fake can satisfy the shared [NewsRepository] contract and that
 * the suspend API is exercisable from common test code on every target.
 */
class NewsRepositoryContractTest {
    @Test
    fun fakeReturnsTheConfiguredArticlesAndRecordsTheCountry() =
        runTest {
            val articles =
                listOf(
                    Article(
                        title = "Headline",
                        description = null,
                        url = "https://example.com/1",
                        imageUrl = null,
                        source = null,
                    ),
                )
            val repository = FakeNewsRepository(configuredResult = Result.success(articles))

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(articles, result.getOrNull())
            assertEquals(listOf("us"), repository.requestedCountries)
        }

    @Test
    fun fakeReturnsATypedFailurePreservingItsCause() =
        runTest {
            val cause = IllegalStateException("boom")
            val repository =
                FakeNewsRepository(
                    configuredResult = Result.failure(NewsFailure.Unknown(cause)),
                )

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue(failure is NewsFailure.Unknown)
            assertTrue(failure.cause === cause)
        }

    @Test
    fun fakePropagatesCancellationInsteadOfReportingAFailure() =
        runTest {
            val repository =
                FakeNewsRepository(
                    configuredResult = Result.success(emptyList()),
                    throwOnCall = CancellationException("cancelled"),
                )

            var thrown: Throwable? = null
            try {
                repository.getTopHeadlines("us")
            } catch (e: CancellationException) {
                thrown = e
            }

            assertTrue(thrown is CancellationException)
            assertEquals("cancelled", thrown.message)
        }

    private class FakeNewsRepository(
        private val configuredResult: Result<List<Article>> = Result.success(emptyList()),
        private val throwOnCall: Throwable? = null,
    ) : NewsRepository {
        private val _requestedCountries = mutableListOf<String>()

        val requestedCountries: List<String>
            get() = _requestedCountries.toList()

        override suspend fun getTopHeadlines(country: String): Result<List<Article>> {
            _requestedCountries += country
            throwOnCall?.let { throw it }
            return configuredResult
        }
    }
}
