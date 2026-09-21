package pl.recipesforsoftware.signalbrief.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * A unique assignment of an article URL to a collection.
 *
 * The composite primary key keeps an assignment unique. [articleId] is the
 * article URL, the stable real-world identity, and is deliberately not a
 * foreign key to `saved_articles` so an assignment survives an article being
 * unsaved. The optional display fields mirror the durable article snapshot
 * stored by [SavedArticleEntity] so a membership stays renderable even after
 * the saved article is removed. Collection deletion is enforced by SQLite
 * through the collection foreign-key cascade.
 */
@Entity(
    tableName = "article_collection_memberships",
    primaryKeys = ["collection_id", "article_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["article_id"])],
)
internal data class ArticleCollectionMembershipEntity(
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    @ColumnInfo(name = "article_id") val articleId: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
)
