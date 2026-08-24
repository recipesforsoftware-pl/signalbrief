package pl.recipesforsoftware.signalbrief.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves a plain fake can satisfy the shared [NewsRepository] contract and that
 * the suspend API — including the typed feed provenance — is exercisable from
 * common test code on every target.
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
            val repository =
                FakeNewsRepository(
                    configuredResult = Result.success(TopHeadlinesFeed(articles, FeedSource.NETWORK)),
                )

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(articles, result.getOrNull()?.articles)
            assertEquals(FeedSource.NETWORK, result.getOrNull()?.source)
            assertEquals(listOf("us"), repository.requestedCountries)
        }

    @Test
    fun fakeExposesTheCacheProvenanceOfTheFeed() =
        runTest {
            val repository =
                FakeNewsRepository(
                    configuredResult =
                        Result.success(
                            TopHeadlinesFeed(
                                articles = emptyList(),
                                source = FeedSource.CACHE,
                            ),
                        ),
                )

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(FeedSource.CACHE, result.getOrNull()?.source)
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
                    configuredResult =
                        Result.success(
                            TopHeadlinesFeed(articles = emptyList(), source = FeedSource.NETWORK),
                        ),
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
        private val configuredResult: Result<TopHeadlinesFeed> =
            Result.success(TopHeadlinesFeed(articles = emptyList(), source = FeedSource.NETWORK)),
        private val throwOnCall: Throwable? = null,
    ) : NewsRepository {
        private val _requestedCountries = mutableListOf<String>()

        val requestedCountries: List<String>
            get() = _requestedCountries.toList()

        override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
            _requestedCountries += country
            throwOnCall?.let { throw it }
            return configuredResult
        }

        override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> =
            flowOf(configuredResult.getOrNull()?.articles.orEmpty())
    }
}
