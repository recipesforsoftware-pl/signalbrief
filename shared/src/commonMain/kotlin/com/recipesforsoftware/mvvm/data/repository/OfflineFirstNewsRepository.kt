package com.recipesforsoftware.mvvm.data.repository

import com.recipesforsoftware.mvvm.data.local.NewsLocalDataSource
import com.recipesforsoftware.mvvm.data.remote.NewsRemoteDataSource
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository

/**
 * Network-first [NewsRepository] with a persistent fallback.
 *
 * Every request hits the remote source first and only then consults the local
 * cache, so fresh data is always preferred and the cache is updated only by a
 * successful remote response. This is deliberately **not**
 * stale-while-revalidate: the UI stays on the cached articles until the next
 * explicit refresh, and a refresh never serves cache while a newer remote
 * response is available.
 *
 * Policy:
 * - remote success replaces the cached country/feed transactionally and returns
 *   the fresh articles (an empty remote list clears that country's cache);
 * - a typed [NewsFailure.Network] falls back to the cached country/feed when it
 *   is non-empty, otherwise the original network failure is preserved;
 * - invalid-data, unknown and other non-network failures are never hidden by
 *   cache;
 * - coroutine cancellation propagates and never reads cache;
 * - a failed remote request never touches the cache;
 * - country caches stay isolated because every local operation is country-scoped.
 */
class OfflineFirstNewsRepository(
    private val remoteDataSource: NewsRemoteDataSource,
    private val localDataSource: NewsLocalDataSource,
) : NewsRepository {
    override suspend fun getTopHeadlines(country: String): Result<List<Article>> {
        val remoteResult = remoteDataSource.getTopHeadlines(country)
        return if (remoteResult.isSuccess) {
            storeRemoteSuccess(country, remoteResult.getOrThrow())
        } else {
            fallBackToCacheWhenOffline(country, remoteResult)
        }
    }

    private suspend fun storeRemoteSuccess(
        country: String,
        articles: List<Article>,
    ): Result<List<Article>> = localDataSource.saveTopHeadlines(country, articles).map { articles }

    private suspend fun fallBackToCacheWhenOffline(
        country: String,
        remoteResult: Result<List<Article>>,
    ): Result<List<Article>> {
        if (remoteResult.exceptionOrNull() !is NewsFailure.Network) {
            return remoteResult
        }
        val cached = localDataSource.getTopHeadlines(country)
        return if (cached.isSuccess && cached.getOrThrow().isNotEmpty()) {
            cached
        } else {
            remoteResult
        }
    }
}
