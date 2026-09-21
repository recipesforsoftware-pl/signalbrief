package pl.recipesforsoftware.signalbrief.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.data.local.dao.SavedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.mapper.toDomain
import pl.recipesforsoftware.signalbrief.data.local.mapper.toSavedEntity
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Room-backed [SavedArticlesRepository].
 *
 * Saved articles live in their own table and are completely independent of the
 * rotating top-headlines cache. Operations are translated into typed
 * [NewsFailure.Unknown] values so the caller never sees a raw Room or SQLite
 * exception; cancellation is always preserved.
 *
 * @param clock returns epoch milliseconds for the [savedAt] timestamp. Injected
 *   so tests can use deterministic values without a large clock abstraction.
 */
class RoomSavedArticlesRepository(
    database: SignalBriefDatabase,
    private val clock: () -> Long = { currentTimeMillis() },
) : SavedArticlesRepository {
    private val dao: SavedArticleDao = database.savedArticleDao()

    override fun observeAllSavedArticles(): Flow<List<Article>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun isArticleSaved(url: String): Flow<Boolean> = dao.observeByUrl(url).map { it != null }

    override suspend fun saveArticle(article: Article): Result<Unit> {
        if (article.url.isBlank()) {
            return Result.failure(NewsFailure.InvalidData)
        }
        return guardLocal {
            dao.insertOrUpdate(article.toSavedEntity(savedAt = clock()))
        }
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> =
        guardLocal {
            dao.deleteByUrl(url)
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> guardLocal(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(NewsFailure.Unknown(e))
        }
}
