package com.recipesforsoftware.mvvm.domain.repository

import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article

/**
 * Domain-facing contract for reading the top-headlines feed.
 *
 * Implementations live in the data layer and are the only code allowed to touch
 * transport DTOs and Retrofit. Domain and UI code consume domain models through
 * this boundary only.
 */
interface NewsRepository {
    /**
     * Returns the latest top headlines for [country] (ISO 3166-1 alpha-2).
     *
     * Failures are returned as a [Result.failure] carrying a [NewsFailure].
     * Coroutine cancellation is always rethrown and never reported as a failure.
     */
    suspend fun getTopHeadlines(country: String): Result<List<Article>>
}
