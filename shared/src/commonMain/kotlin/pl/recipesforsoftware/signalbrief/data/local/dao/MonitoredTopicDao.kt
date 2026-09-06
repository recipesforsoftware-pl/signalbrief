package pl.recipesforsoftware.signalbrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.data.local.entity.MonitoredTopicEntity

/**
 * DAO for the monitored topics table.
 *
 * All public operations are scoped to a single monitored topic identified by
 * its auto-generated id. Callers outside this module should never see the
 * entity directly.
 */
@Dao
internal interface MonitoredTopicDao {
    /**
     * Returns all monitored topics ordered by normalized query ascending, with
     * [id] ascending as a stable tie-breaker for equal normalized queries.
     */
    @Query(
        """
        SELECT * FROM monitored_topics
        ORDER BY normalized_query ASC, id ASC
        """,
    )
    fun observeAll(): Flow<List<MonitoredTopicEntity>>

    /** Returns whether a monitored topic with [id] exists. */
    @Query("SELECT EXISTS(SELECT 1 FROM monitored_topics WHERE id = :id)")
    suspend fun existsById(id: Long): Boolean

    /** Returns whether any monitored topic has [normalizedQuery]. */
    @Query("SELECT EXISTS(SELECT 1 FROM monitored_topics WHERE normalized_query = :normalizedQuery)")
    suspend fun existsByNormalizedQuery(normalizedQuery: String): Boolean

    /**
     * Returns whether any monitored topic other than [id] has
     * [normalizedQuery]. Used to detect an update that would collide with a
     * different topic while allowing a topic to keep its own query.
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM monitored_topics" +
            " WHERE normalized_query = :normalizedQuery AND id != :id)",
    )
    suspend fun existsByNormalizedQueryExcluding(
        normalizedQuery: String,
        id: Long,
    ): Boolean

    /**
     * Inserts a new monitored topic and returns the auto-generated row id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: MonitoredTopicEntity): Long

    /**
     * Updates the query of the monitored topic with the given [id].
     *
     * Returns the number of rows affected (0 when no row matches [id]).
     */
    @Query("UPDATE monitored_topics SET query = :query, normalized_query = :normalizedQuery WHERE id = :id")
    suspend fun updateQuery(
        id: Long,
        query: String,
        normalizedQuery: String,
    ): Int

    /**
     * Deletes the monitored topic with the given [id].
     *
     * Returns the number of rows affected (0 when no row matches [id]).
     */
    @Query("DELETE FROM monitored_topics WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all monitored topics. Used for test teardown and data management.
     */
    @Query("DELETE FROM monitored_topics")
    suspend fun clearAll()
}
