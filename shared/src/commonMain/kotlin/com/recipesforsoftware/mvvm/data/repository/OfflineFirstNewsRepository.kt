package com.recipesforsoftware.mvvm.data.repository

import com.recipesforsoftware.mvvm.data.local.NewsLocalDataSource
import com.recipesforsoftware.mvvm.data.remote.NewsRemoteDataSource
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.FeedSource
import com.recipesforsoftware.mvvm.domain.model.TopHeadlinesFeed
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
 * - remote success is deduplicated by URL (first occurrence wins, original order
 *   preserved), the unique list replaces the cached country/feed transactionally,
 *   and the fresh articles are returned typed as [FeedSource.NETWORK] (an empty
 *   remote list clears that country's cache);
 * - a typed [NewsFailure.Network] falls back to the cached country/feed typed
 *   as [FeedSource.CACHE] when it is non-empty, otherwise the original network
 *   failure is preserved;
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
    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
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
    ): Result<TopHeadlinesFeed> {
        val uniqueArticles = articles.distinctByUrl()
        val saved = localDataSource.saveTopHeadlines(country, uniqueArticles)
        return saved.map { TopHeadlinesFeed(uniqueArticles, FeedSource.NETWORK) }
    }

    private suspend fun fallBackToCacheWhenOffline(
        country: String,
        remoteResult: Result<List<Article>>,
    ): Result<TopHeadlinesFeed> {
        val failure = remoteResult.exceptionOrNull()
        return when {
            failure == null -> {
                remoteResult.map { TopHeadlinesFeed(it.distinctByUrl(), FeedSource.NETWORK) }
            }

            failure !is NewsFailure.Network -> {
                Result.failure(failure)
            }

            else -> {
                cachedFeedOrOriginalFailure(country, failure)
            }
        }
    }

    private suspend fun cachedFeedOrOriginalFailure(
        country: String,
        failure: NewsFailure.Network,
    ): Result<TopHeadlinesFeed> {
        val cached = localDataSource.getTopHeadlines(country)
        return if (cached.isSuccess && cached.getOrThrow().isNotEmpty()) {
            cached.map { TopHeadlinesFeed(it.distinctByUrl(), FeedSource.CACHE) }
        } else {
            Result.failure(failure)
        }
    }
}

private fun List<Article>.distinctByUrl(): List<Article> = distinctBy { it.url }
