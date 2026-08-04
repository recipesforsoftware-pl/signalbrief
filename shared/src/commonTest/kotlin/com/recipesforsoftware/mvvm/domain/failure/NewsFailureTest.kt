package com.recipesforsoftware.mvvm.domain.failure

import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import kotlinx.coroutines.test.runTest
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
            val expected: Result<List<Article>> = Result.failure(NewsFailure.Network)
            val repository =
                object : NewsRepository {
                    override suspend fun getTopHeadlines(country: String): Result<List<Article>> = expected
                }

            val result = repository.getTopHeadlines("us")

            assertEquals(NewsFailure.Network, result.exceptionOrNull())
        }
}
