package pl.recipesforsoftware.signalbrief.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.data.local.dao.ArticleCollectionMembershipDao
import pl.recipesforsoftware.signalbrief.data.local.dao.CollectionDao
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.mapper.newCollectionEntity
import pl.recipesforsoftware.signalbrief.data.local.mapper.toDomain
import pl.recipesforsoftware.signalbrief.data.local.mapper.toMembershipEntity
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.CollectionName
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Room-backed [CollectionsRepository].
 *
 * Collections live in their own table and are completely independent of cached
 * articles and saved articles. Operations are translated into typed
 * [CollectionFailure] values so the caller never sees a raw Room or SQLite
 * exception; cancellation is always preserved.
 *
 * @param clock returns epoch milliseconds for the [created_at] timestamp.
 *   Injected so tests can use deterministic values without a large clock
 *   abstraction.
 */
class RoomCollectionsRepository(
    database: SignalBriefDatabase,
    private val clock: () -> Long = { currentTimeMillis() },
) : CollectionsRepository {
    private val dao: CollectionDao = database.collectionDao()
    private val membershipDao: ArticleCollectionMembershipDao = database.articleCollectionMembershipDao()

    override fun observeAllCollections(): Flow<List<Collection>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeCollectionIdsForArticle(articleId: String): Flow<Set<String>> =
        membershipDao.observeCollectionIdsForArticle(articleId).map { ids -> ids.map(Long::toString).toSet() }

    override suspend fun createCollection(name: String): Result<Collection> {
        val collectionName =
            CollectionName.from(name)
                ?: return Result.failure(CollectionFailure.InvalidName)
        return guardLocal {
            val id =
                dao.insert(
                    newCollectionEntity(
                        name = collectionName.value,
                        createdAt = clock(),
                    ),
                )
            Collection(
                id = id.toString(),
                name = collectionName.value,
            )
        }
    }

    override suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection> {
        val collectionName = CollectionName.from(newName)
        val rowId = id.toLongOrNull()
        if (collectionName == null || rowId == null) {
            return Result.failure(
                if (collectionName == null) CollectionFailure.InvalidName else CollectionFailure.NotFound,
            )
        }
        return guardLocal {
            val affected = dao.updateName(rowId, collectionName.value)
            if (affected == 0) throw CollectionFailure.NotFound
            Collection(rowId.toString(), collectionName.value)
        }
    }

    override suspend fun deleteCollection(id: String): Result<Unit> {
        val rowId =
            id.toLongOrNull()
                ?: return Result.failure(CollectionFailure.NotFound)
        return guardLocal {
            val affected = dao.deleteById(rowId)
            if (affected == 0) throw CollectionFailure.NotFound
        }
    }

    override suspend fun addArticleToCollection(
        article: Article,
        collectionId: String,
    ): Result<Unit> {
        val rowId = membershipCollectionId(article.url, collectionId).getOrElse { return Result.failure(it) }
        return guardLocal {
            if (!dao.exists(rowId)) throw CollectionFailure.NotFound
            membershipDao.insertIgnore(article.toMembershipEntity(rowId))
        }
    }

    override suspend fun removeArticleFromCollection(
        articleId: String,
        collectionId: String,
    ): Result<Unit> {
        val rowId = membershipCollectionId(articleId, collectionId).getOrElse { return Result.failure(it) }
        return guardLocal {
            if (!dao.exists(rowId)) throw CollectionFailure.NotFound
            membershipDao.delete(collectionId = rowId, articleId = articleId)
        }
    }

    private fun membershipCollectionId(
        articleId: String,
        collectionId: String,
    ): Result<Long> =
        when {
            articleId.isBlank() -> Result.failure(CollectionFailure.InvalidArticleId)
            collectionId.toLongOrNull() == null -> Result.failure(CollectionFailure.NotFound)
            else -> Result.success(collectionId.toLong())
        }

    /**
     * Executes [block] and returns it as a [Result]. Cancellation is always
     * rethrown. [CollectionFailure] exceptions are propagated as typed domain
     * failures. Any other exception is wrapped in [Result.failure] as an
     * [IllegalStateException].
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> guardLocal(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CollectionFailure) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Collections persistence failure", e))
        }
}
