package pl.recipesforsoftware.signalbrief.domain.repository

import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed

/**
 * Domain-facing contract for reading the top-headlines feed.
 *
 * Implementations live in the data layer and are the only code allowed to touch
 * transport DTOs and Ktor. Domain and UI code consume domain models through
 * this boundary only.
 */
interface NewsRepository {
    /**
     * Returns the latest top headlines for [country] (ISO 3166-1 alpha-2).
     *
     * The feed is typed with its [pl.recipesforsoftware.signalbrief.domain.model.FeedSource]
     * so callers can tell fresh network content apart from a cache fallback.
     * Failures are returned as a [Result.failure] carrying a [NewsFailure].
     * Coroutine cancellation is always rethrown and never reported as a failure.
     */
    suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed>
}
