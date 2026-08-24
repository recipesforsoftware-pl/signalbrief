package pl.recipesforsoftware.signalbrief.domain.failure

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NewsFailureTest {
    @Test
    fun unknownPreservesItsCause() {
        val cause = IllegalStateException("boom")

        val failure = NewsFailure.Unknown(cause)

        assertTrue(failure.cause === cause)
        assertEquals("boom", failure.cause.message)
    }

    @Test
    fun networkAndInvalidDataAreDistinctSingletonFailures() {
        val network: NewsFailure = NewsFailure.Network
        val invalidData: NewsFailure = NewsFailure.InvalidData

        assertNotEquals(network, invalidData)
        assertEquals(NewsFailure.Network, network)
        assertEquals(NewsFailure.InvalidData, invalidData)
        assertTrue(NewsFailure.Network === network)
    }

    @Test
    fun unknownEqualityIsBasedOnTheWrappedCause() {
        val sameCause = IllegalStateException("boom")

        assertEquals(NewsFailure.Unknown(sameCause), NewsFailure.Unknown(sameCause))

        assertNotEquals(
            NewsFailure.Unknown(IllegalStateException("boom")),
            NewsFailure.Unknown(IllegalStateException("boom")),
        )
    }

    @Test
    fun typedFailuresAreUsableThroughTheRepositoryContract() =
        runTest {
            val expected: Result<TopHeadlinesFeed> = Result.failure(NewsFailure.Network)
            val repository =
                object : NewsRepository {
                    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> = expected

                    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = flowOf(emptyList())
                }

            val result = repository.getTopHeadlines("us")

            assertEquals(NewsFailure.Network, result.exceptionOrNull())
        }
}
