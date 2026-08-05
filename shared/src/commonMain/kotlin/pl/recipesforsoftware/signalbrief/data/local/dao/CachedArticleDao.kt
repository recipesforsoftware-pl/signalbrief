package pl.recipesforsoftware.signalbrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pl.recipesforsoftware.signalbrief.data.local.entity.CachedArticleEntity

/**
 * DAO for the cached top-headlines table.
 *
 * All public operations are scoped to a country/feed pair. Callers outside this
 * module should never see the entity directly.
 */
@Dao
internal interface CachedArticleDao {
    /**
     * Returns all cached articles for [country] and [feed] in the order they were
     * originally inserted, i.e. by [CachedArticleEntity.positionInFeed].
     */
    @Query(
        """
        SELECT * FROM cached_articles
        WHERE country = :country AND feed = :feed
        ORDER BY position_in_feed ASC
        """,
    )
    suspend fun getByCountryAndFeed(
        country: String,
        feed: String,
    ): List<CachedArticleEntity>

    /**
     * Removes every cached article for [country] and [feed].
     */
    @Query("DELETE FROM cached_articles WHERE country = :country AND feed = :feed")
    suspend fun deleteByCountryAndFeed(
        country: String,
        feed: String,
    )

    /**
     * Inserts or replaces a list of cached articles.
     *
     * On conflict the existing row is overwritten, so the same URL is never
     * duplicated within a country/feed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<CachedArticleEntity>)

    /**
     * Atomically replaces the cached articles for [country] and [feed].
     *
     * The delete and insert run inside a single Room transaction, so readers
     * never observe a partially populated feed.
     */
    @Transaction
    suspend fun replaceAll(
        country: String,
        feed: String,
        articles: List<CachedArticleEntity>,
    ) {
        deleteByCountryAndFeed(country, feed)
        insertAll(articles)
    }

    /**
     * Deletes all cached articles. Useful mainly for tests.
     */
    @Query("DELETE FROM cached_articles")
    suspend fun clearAll()
}
