package com.recipesforsoftware.mvvm.data.local

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article

/**
 * Local-facing contract for the persistent top-headlines cache.
 *
 * Only domain models cross this boundary: database entities, DAOs and Room stay
 * behind the implementation. Failures are typed as [NewsFailure.Unknown] wrapping
 * the database cause; coroutine cancellation is always rethrown.
 */
interface NewsLocalDataSource {
    /**
     * Reads the cached top headlines for [country] (ISO 3166-1 alpha-2) in feed
     * order. An absent or empty cache returns a successful empty list.
     */
    suspend fun getTopHeadlines(country: String): Result<List<Article>>

    /**
     * Transactionally replaces the cached top headlines for [country]. Passing an
     * empty list clears that country's cache. A failed remote request must never
     * reach this method.
     */
    suspend fun saveTopHeadlines(
        country: String,
        articles: List<Article>,
    ): Result<Unit>
}
