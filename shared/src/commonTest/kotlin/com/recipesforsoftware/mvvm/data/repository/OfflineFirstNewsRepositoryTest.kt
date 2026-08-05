package com.recipesforsoftware.mvvm.data.repository

import com.recipesforsoftware.mvvm.data.local.NewsLocalDataSource
import com.recipesforsoftware.mvvm.data.remote.NewsRemoteDataSource
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.FeedSource
import com.recipesforsoftware.mvvm.domain.model.Source
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Behavioral tests for [OfflineFirstNewsRepository] over fake remote and local
 * boundaries. No live network and no Room instance is involved: the policy is
 * exercised entirely through the two data-source contracts.
 */
class OfflineFirstNewsRepositoryTest {
    private val remote = FakeRemoteDataSource()
    private val local = FakeLocalDataSource()
    private val repository = OfflineFirstNewsRepository(remote, local)

    @Test
    fun remoteSuccessReturnsFreshDataAndStoresIt() =
        runTest {
            val fresh = articles("https://example.com/1", "https://example.com/2")
            remote.nextResult = Result.success(fresh)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(fresh, result.getOrNull()?.articles)
            assertEquals(FeedSource.NETWORK, result.getOrNull()?.source)
            assertEquals(fresh, local.cached("us"))
            assertEquals(listOf("us" to fresh), local.saveCalls)
        }

    @Test
    fun remoteSuccessDeduplicatesByUrlAndPreservesFirstOccurrenceAndOrder() =
        runTest {
            val first = article(url = "https://example.com/1", title = "First")
            val second = article(url = "https://example.com/2", title = "Second")
            val duplicate = article(url = "https://example.com/1", title = "Duplicate")
            val remoteArticles = listOf(first, second, duplicate)
            remote.nextResult = Result.success(remoteArticles)

            val result = repository.getTopHeadlines("us")

            val expected = listOf(first, second)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull()?.articles)
            assertEquals(FeedSource.NETWORK, result.getOrNull()?.source)
            assertEquals(expected, local.cached("us"))
            assertEquals(listOf("us" to expected), local.saveCalls)
        }

    @Test
    fun remoteSuccessDeduplicationRemovesOnlyDuplicatesAndKeepsEmptyList() =
        runTest {
            remote.nextResult = Result.success(emptyList())

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.articles?.isEmpty() == true)
            assertTrue(local.cached("us").isEmpty())
        }

    @Test
    fun secondSuccessReplacesTheOldCache() =
        runTest {
            local.seed("us", articles("https://example.com/old"))
            val newer = articles("https://example.com/new")
            remote.nextResult = Result.success(newer)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(newer, result.getOrNull()?.articles)
            assertEquals(FeedSource.NETWORK, result.getOrNull()?.source)
            assertEquals(newer, local.cached("us"))
        }

    @Test
    fun remoteEmptyListClearsTheCachedCountryAndReturnsEmpty() =
        runTest {
            local.seed("us", articles("https://example.com/old"))
            remote.nextResult = Result.success(emptyList())

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.articles?.isEmpty() == true)
            assertEquals(
                FeedSource.NETWORK,
                result.getOrNull()?.source,
                "An empty remote success is still a fresh network feed",
            )
            assertTrue(local.cached("us").isEmpty())
        }

    @Test
    fun networkFailureWithCacheReturnsTheCache() =
        runTest {
            val cached = articles("https://example.com/cached")
            local.seed("us", cached)
            remote.nextResult = Result.failure(NewsFailure.Network)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals(cached, result.getOrNull()?.articles)
            assertEquals(FeedSource.CACHE, result.getOrNull()?.source)
            assertTrue(local.saveCalls.isEmpty(), "A failed remote request must not touch the cache")
        }

    @Test
    fun networkFailureWithDuplicateCacheReturnsUniqueFirstOccurrenceAndPreservesOrder() =
        runTest {
            val first = article(url = "https://example.com/1", title = "First")
            val second = article(url = "https://example.com/2", title = "Second")
            val duplicate = article(url = "https://example.com/1", title = "Duplicate")
            local.seed("us", listOf(first, second, duplicate))
            remote.nextResult = Result.failure(NewsFailure.Network)

            val result = repository.getTopHeadlines("us")

            val expected = listOf(first, second)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull()?.articles)
            assertEquals(FeedSource.CACHE, result.getOrNull()?.source)
            assertTrue(local.saveCalls.isEmpty(), "A failed remote request must not touch the cache")
        }

    @Test
    fun networkFailureWithoutCacheReturnsTheOriginalNetworkFailure() =
        runTest {
            remote.nextResult = Result.failure(NewsFailure.Network)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertSame(NewsFailure.Network, result.exceptionOrNull())
        }

    @Test
    fun invalidDataFailureIsNotHiddenByCache() =
        runTest {
            local.seed("us", articles("https://example.com/cached"))
            remote.nextResult = Result.failure(NewsFailure.InvalidData)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertSame(NewsFailure.InvalidData, result.exceptionOrNull())
        }

    @Test
    fun unknownFailureIsNotHiddenByCacheAndPreservesItsCause() =
        runTest {
            val cause = IllegalStateException("boom")
            local.seed("us", articles("https://example.com/cached"))
            remote.nextResult = Result.failure(NewsFailure.Unknown(cause))

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue(failure is NewsFailure.Unknown)
            assertSame(cause, failure.cause)
        }

    @Test
    fun cancellationPropagatesWithoutReadingTheCache() =
        runTest {
            local.seed("us", articles("https://example.com/cached"))
            remote.throwOnCall = CancellationException("cancelled")

            assertFailsWith<CancellationException> {
                repository.getTopHeadlines("us")
            }

            assertTrue(local.readCalls.isEmpty(), "Cache must not be read after cancellation")
        }

    @Test
    fun failedRemoteRequestDoesNotCorruptTheExistingCache() =
        runTest {
            val cached = articles("https://example.com/cached")
            local.seed("us", cached)

            remote.nextResult = Result.failure(NewsFailure.Network)
            repository.getTopHeadlines("us")

            remote.nextResult = Result.failure(NewsFailure.InvalidData)
            repository.getTopHeadlines("us")

            assertEquals(cached, local.cached("us"))
            assertTrue(local.saveCalls.isEmpty())
        }

    @Test
    fun countryCachesRemainIsolated() =
        runTest {
            val usArticles = articles("https://example.com/us")
            val plArticles = articles("https://example.com/pl")
            local.seed("us", usArticles)
            local.seed("pl", plArticles)

            remote.nextResult = Result.failure(NewsFailure.Network)
            val usResult = repository.getTopHeadlines("us")
            val plResult = repository.getTopHeadlines("pl")

            assertTrue(usResult.isSuccess)
            assertTrue(plResult.isSuccess)
            assertEquals(usArticles, usResult.getOrNull()?.articles)
            assertEquals(plArticles, plResult.getOrNull()?.articles)

            local.seed("us", articles("https://example.com/us-refreshed"))
            assertEquals(plArticles, local.cached("pl"))
        }

    @Test
    fun localReadFailureIsHandledAndTheNetworkFailureIsPreserved() =
        runTest {
            remote.nextResult = Result.failure(NewsFailure.Network)
            local.nextReadFailure = NewsFailure.Unknown(IllegalStateException("database unavailable"))

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertSame(
                NewsFailure.Network,
                result.exceptionOrNull(),
                "An unreadable cache must not crash and must preserve the original network failure",
            )
        }

    @Test
    fun localSaveFailureIsReportedAsATypedFailureAfterRemoteSuccess() =
        runTest {
            remote.nextResult = Result.success(articles("https://example.com/1"))
            val dbCause = IllegalStateException("database unavailable")
            local.nextSaveFailure = NewsFailure.Unknown(dbCause)

            val result = repository.getTopHeadlines("us")

            assertTrue(result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue(failure is NewsFailure.Unknown)
            assertSame(dbCause, failure.cause)
        }

    private class FakeRemoteDataSource : NewsRemoteDataSource {
        var nextResult: Result<List<Article>> = Result.success(emptyList())
        var throwOnCall: Throwable? = null

        override suspend fun getTopHeadlines(country: String): Result<List<Article>> {
            throwOnCall?.let { throw it }
            return nextResult
        }
    }

    private class FakeLocalDataSource : NewsLocalDataSource {
        private val caches = mutableMapOf<String, List<Article>>()
        val saveCalls = mutableListOf<Pair<String, List<Article>>>()
        val readCalls = mutableListOf<String>()
        var nextReadFailure: NewsFailure? = null
        var nextSaveFailure: NewsFailure? = null

        override suspend fun getTopHeadlines(country: String): Result<List<Article>> {
            readCalls += country
            nextReadFailure?.let { return Result.failure(it) }
            return Result.success(caches[country].orEmpty())
        }

        override suspend fun saveTopHeadlines(
            country: String,
            articles: List<Article>,
        ): Result<Unit> {
            saveCalls += country to articles
            nextSaveFailure?.let { return Result.failure(it) }
            caches[country] = articles
            return Result.success(Unit)
        }

        fun cached(country: String): List<Article> = caches[country].orEmpty()

        fun seed(
            country: String,
            articles: List<Article>,
        ) {
            caches[country] = articles
        }
    }

    private companion object {
        private val source = Source(id = "test", name = "Test News")

        fun article(
            url: String,
            title: String = "Headline",
        ): Article =
            Article(
                title = title,
                description = "Description",
                url = url,
                imageUrl = null,
                source = source,
            )

        fun articles(vararg urls: String): List<Article> =
            urls.mapIndexed { index, url ->
                article(
                    url = url,
                    title = "Headline $index",
                )
            }
    }
}
