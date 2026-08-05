package pl.recipesforsoftware.signalbrief.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Room persistence model for a cached top-headline article.
 *
 * The primary key is a composite of country, feed and the article URL. The URL
 * is the stable real-world identity of an article; country and feed are added
 * so the same article can be cached independently for different queries.
 *
 * [positionInFeed] preserves the remote ordering within a single country/feed
 * response and is included in the query index.
 */
@Entity(
    tableName = "cached_articles",
    primaryKeys = ["country", "feed", "url"],
    indices = [
        Index(value = ["country", "feed", "position_in_feed"]),
    ],
)
internal data class CachedArticleEntity(
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "feed") val feed: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "position_in_feed") val positionInFeed: Int,
)
