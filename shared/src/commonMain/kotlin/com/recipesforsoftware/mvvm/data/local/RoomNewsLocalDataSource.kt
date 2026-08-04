package com.recipesforsoftware.mvvm.data.local

import com.recipesforsoftware.mvvm.data.local.db.SignalBriefDatabase
import com.recipesforsoftware.mvvm.data.local.mapper.toDomain
import com.recipesforsoftware.mvvm.data.local.mapper.toEntity
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * Room-backed [NewsLocalDataSource].
 *
 * Reads and writes the top-headlines cache through [SignalBriefDatabase] and its
 * DAO. The DAO scopes every operation to a country/feed pair, keeps the remote
 * ordering stable via `position_in_feed`, and `replaceAll` swaps a country's
 * cache transactionally. Exceptions from the database are translated into typed
 * [NewsFailure.Unknown] values so the repository never sees a raw Room or
 * SQLite exception; cancellation is preserved.
 */
class RoomNewsLocalDataSource(
    database: SignalBriefDatabase,
) : NewsLocalDataSource {
    private val dao = database.articleDao()

    override suspend fun getTopHeadlines(country: String): Result<List<Article>> =
        guardLocal {
            dao.getByCountryAndFeed(country, TOP_HEADLINES_FEED).map { it.toDomain() }
        }

    override suspend fun saveTopHeadlines(
        country: String,
        articles: List<Article>,
    ): Result<Unit> =
        guardLocal {
            dao.replaceAll(
                country = country,
                feed = TOP_HEADLINES_FEED,
                articles =
                    articles.mapIndexed { position, article ->
                        article.toEntity(
                            country = country,
                            feed = TOP_HEADLINES_FEED,
                            positionInFeed = position,
                        )
                    },
            )
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> guardLocal(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            // Room cancels its internal query job when the database closes, which
            // surfaces as a cancellation-like exception even though the caller is
            // still active. Only a cancellation of the caller must propagate.
            coroutineContext.ensureActive()
            Result.failure(NewsFailure.Unknown(e))
        } catch (e: Exception) {
            Result.failure(NewsFailure.Unknown(e))
        }
}
