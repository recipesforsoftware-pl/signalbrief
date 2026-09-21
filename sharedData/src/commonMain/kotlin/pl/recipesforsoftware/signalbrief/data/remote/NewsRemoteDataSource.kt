package pl.recipesforsoftware.signalbrief.data.remote

import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Remote-facing contract for fetching the top-headlines feed.
 *
 * Implementations translate transport and serialization errors into typed
 * [NewsFailure] values and always rethrow coroutine cancellation. Domain code
 * never sees a transport or serialization type through this boundary.
 */
interface NewsRemoteDataSource {
    /**
     * Fetches the latest top headlines for [country] (ISO 3166-1 alpha-2).
     *
     * Failures are returned as a [Result.failure] carrying a [NewsFailure].
     * Coroutine cancellation is always rethrown and never reported as a failure.
     */
    suspend fun getTopHeadlines(country: String): Result<List<Article>>
}
