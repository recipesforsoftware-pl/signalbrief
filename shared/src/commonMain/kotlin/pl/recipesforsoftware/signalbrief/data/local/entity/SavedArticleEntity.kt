package pl.recipesforsoftware.signalbrief.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Room persistence model for a user-saved article.
 *
 * The primary key is the article URL, which is the stable real-world identity.
 * Saved articles are independent of the rotating top-headlines cache and must
 * survive normal cache refresh, replacement, and eviction.
 *
 * [savedAt] is a Unix-epoch millisecond timestamp used for most-recently-saved
 * first ordering. It is stored at insert time and never updated on re-save.
 */
@Entity(
    tableName = "saved_articles",
    primaryKeys = ["url"],
    indices = [
        Index(value = ["saved_at"]),
    ],
)
internal data class SavedArticleEntity(
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "saved_at") val savedAt: Long,
)
