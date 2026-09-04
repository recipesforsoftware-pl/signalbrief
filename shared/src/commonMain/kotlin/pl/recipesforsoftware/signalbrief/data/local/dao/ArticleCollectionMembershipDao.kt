package pl.recipesforsoftware.signalbrief.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.data.local.entity.ArticleCollectionMembershipEntity

/** DAO for article-to-collection membership relations. */
@Dao
internal interface ArticleCollectionMembershipDao {
    @Query(
        "SELECT collection_id FROM article_collection_memberships " +
            "WHERE article_id = :articleId ORDER BY collection_id ASC",
    )
    fun observeCollectionIdsForArticle(articleId: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ArticleCollectionMembershipEntity): Long

    @Query(
        "DELETE FROM article_collection_memberships " +
            "WHERE collection_id = :collectionId AND article_id = :articleId",
    )
    suspend fun delete(
        collectionId: Long,
        articleId: String,
    ): Int
}
