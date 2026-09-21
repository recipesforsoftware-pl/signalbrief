package pl.recipesforsoftware.signalbrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.data.local.entity.SavedArticleEntity

/**
 * DAO for the user-saved articles table.
 *
 * All public operations are scoped to a single article identified by its URL.
 * Callers outside this module should never see the entity directly.
 */
@Dao
internal interface SavedArticleDao {
    /**
     * Returns all saved articles ordered by most recently saved first.
     */
    @Query(
        """
        SELECT * FROM saved_articles
        ORDER BY saved_at DESC
        """,
    )
    fun observeAll(): Flow<List<SavedArticleEntity>>

    /**
     * Returns all saved articles once (non-reactive snapshot). Used for
     * one-shot reads and test assertions.
     */
    @Query(
        """
        SELECT * FROM saved_articles
        ORDER BY saved_at DESC
        """,
    )
    suspend fun getAllOnce(): List<SavedArticleEntity>

    /**
     * Returns the saved article with the given [url], or null if not saved.
     */
    @Query("SELECT * FROM saved_articles WHERE url = :url")
    fun observeByUrl(url: String): Flow<SavedArticleEntity?>

    /**
     * Inserts or replaces a saved article. On conflict the existing row is
     * overwritten. The [SavedArticleEntity.savedAt] timestamp is set by the
     * caller at insert time and is replaced on re-save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(article: SavedArticleEntity)

    /**
     * Removes the saved article with the given [url].
     */
    @Query("DELETE FROM saved_articles WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    /**
     * Deletes all saved articles. Used for test teardown and data management.
     */
    @Query("DELETE FROM saved_articles")
    suspend fun clearAll()
}
