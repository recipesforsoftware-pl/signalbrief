package pl.recipesforsoftware.signalbrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.data.local.entity.CollectionEntity

/**
 * DAO for the user collections table.
 *
 * All public operations are scoped to a single collection identified by its
 * auto-generated id. Callers outside this module should never see the entity
 * directly.
 */
@Dao
internal interface CollectionDao {
    /**
     * Returns all collections ordered by newest created first, with [id]
     * DESC as a stable tie-breaker when timestamps are equal.
     */
    @Query(
        """
        SELECT * FROM collections
        ORDER BY created_at DESC, id DESC
        """,
    )
    fun observeAll(): Flow<List<CollectionEntity>>

    /**
     * Inserts a new collection and returns the auto-generated row id.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CollectionEntity): Long

    /**
     * Renames the collection with the given [id] to [newName].
     *
     * Returns the number of rows affected (0 when no row matches [id]).
     */
    @Query("UPDATE collections SET name = :newName WHERE id = :id")
    suspend fun updateName(
        id: Long,
        newName: String,
    ): Int

    /**
     * Deletes the collection with the given [id].
     *
     * Returns the number of rows affected (0 when no row matches [id]).
     */
    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes all collections. Used for test teardown and data management.
     */
    @Query("DELETE FROM collections")
    suspend fun clearAll()
}
