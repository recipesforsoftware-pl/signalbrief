package pl.recipesforsoftware.signalbrief.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

/**
 * Domain-facing contract for the user's monitored topics.
 *
 * Monitored topics are framework-independent domain values. Implementations
 * live in the data layer and assign each topic a stable, opaque [id] (a string
 * at the domain boundary).
 *
 * Query rule: queries are normalized by trimming leading and trailing
 * whitespace. A blank-only query is rejected; otherwise the normalized
 * (trimmed) query is preserved with its original casing. Duplicate detection is
 * deterministic and case-insensitive: two queries that trim to the same text
 * and differ only in casing represent the same monitored topic and are
 * rejected.
 *
 * Ordering is cross-platform deterministic: monitored topics are ordered by
 * normalized query ascending (alphabetical), using the topic id as a stable
 * tie-breaker for equal normalized queries.
 */
interface TopicMonitoringRepository {
    /**
     * Returns all monitored topics.
     *
     * The returned [Flow] emits the current topics immediately and again on
     * every create, update, or delete. Results are ordered by normalized query
     * ascending, then id ascending.
     */
    fun observeTopics(): Flow<List<MonitoredTopic>>

    /**
     * Creates a monitored topic for the given [query].
     *
     * The query is normalized (trimmed) and a blank-only query is rejected with
     * [TopicMonitoringFailure.InvalidQuery]. A query whose trimmed text already
     * exists (case-insensitively) is rejected with
     * [TopicMonitoringFailure.DuplicateQuery]. On success the created
     * [MonitoredTopic] is returned with its repository-assigned unique id and
     * normalized query.
     */
    suspend fun createTopic(query: String): Result<MonitoredTopic>

    /**
     * Updates the query of the monitored topic with the given [id].
     *
     * The new query is normalized (trimmed); a blank-only query is rejected
     * with [TopicMonitoringFailure.InvalidQuery]. Updating the topic to its own
     * current normalized query succeeds (idempotent). Updating to another
     * topic's normalized query is rejected with
     * [TopicMonitoringFailure.DuplicateQuery]. A missing [id] fails with
     * [TopicMonitoringFailure.NotFound]. On success the updated [MonitoredTopic]
     * is returned.
     */
    suspend fun updateTopic(
        id: String,
        query: String,
    ): Result<MonitoredTopic>

    /**
     * Deletes the monitored topic with the given [id].
     *
     * A missing [id] fails with [TopicMonitoringFailure.NotFound]. On success
     * the topic is removed from [observeTopics].
     */
    suspend fun deleteTopic(id: String): Result<Unit>
}
