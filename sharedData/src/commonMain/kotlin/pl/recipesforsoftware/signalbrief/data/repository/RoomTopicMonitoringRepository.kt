package pl.recipesforsoftware.signalbrief.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.data.local.dao.MonitoredTopicDao
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.mapper.newMonitoredTopicEntity
import pl.recipesforsoftware.signalbrief.data.local.mapper.toDomain
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.TopicQuery
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Room-backed [TopicMonitoringRepository].
 *
 * Monitored topics live in their own table and are completely independent of
 * cached articles, saved articles, and collections. Operations are translated
 * into typed [TopicMonitoringFailure] values so the caller never sees a raw
 * Room or SQLite exception; cancellation is always preserved.
 *
 * Deterministic ordering: topics are ordered by normalized query ascending,
 * with id ascending as a stable tie-breaker. Duplicate detection compares the
 * normalized (trimmed, lowercased) query so casing differences do not create
 * separate topics.
 */
class RoomTopicMonitoringRepository(
    database: SignalBriefDatabase,
) : TopicMonitoringRepository {
    private val dao: MonitoredTopicDao = database.monitoredTopicDao()

    override fun observeTopics(): Flow<List<MonitoredTopic>> =
        dao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createTopic(query: String): Result<MonitoredTopic> {
        val topicQuery = TopicQuery.from(query) ?: return Result.failure(TopicMonitoringFailure.InvalidQuery)
        return guardLocal {
            if (dao.existsByNormalizedQuery(topicQuery.normalizedKey)) {
                throw TopicMonitoringFailure.DuplicateQuery
            }
            val id =
                dao.insert(
                    newMonitoredTopicEntity(
                        query = topicQuery.value,
                        normalizedQuery = topicQuery.normalizedKey,
                    ),
                )
            MonitoredTopic(
                id = id.toString(),
                query = topicQuery.value,
            )
        }
    }

    override suspend fun updateTopic(
        id: String,
        query: String,
    ): Result<MonitoredTopic> {
        val topicQuery = TopicQuery.from(query)
        val rowId = id.toLongOrNull()
        if (topicQuery == null || rowId == null) {
            return Result.failure(
                if (topicQuery == null) TopicMonitoringFailure.InvalidQuery else TopicMonitoringFailure.NotFound,
            )
        }
        return guardLocal {
            if (!dao.existsById(rowId)) throw TopicMonitoringFailure.NotFound
            if (dao.existsByNormalizedQueryExcluding(topicQuery.normalizedKey, rowId)) {
                throw TopicMonitoringFailure.DuplicateQuery
            }
            dao.updateQuery(rowId, topicQuery.value, topicQuery.normalizedKey)
            MonitoredTopic(
                id = rowId.toString(),
                query = topicQuery.value,
            )
        }
    }

    override suspend fun deleteTopic(id: String): Result<Unit> {
        val rowId =
            id.toLongOrNull()
                ?: return Result.failure(TopicMonitoringFailure.NotFound)
        return guardLocal {
            val affected = dao.deleteById(rowId)
            if (affected == 0) throw TopicMonitoringFailure.NotFound
        }
    }

    /**
     * Executes [block] and returns it as a [Result]. Cancellation is always
     * rethrown. [TopicMonitoringFailure] exceptions are propagated as typed
     * domain failures. Any other exception is wrapped in [Result.failure] as an
     * [IllegalStateException].
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> guardLocal(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: TopicMonitoringFailure) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Topic monitoring persistence failure", e))
        }
}
