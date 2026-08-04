package com.recipesforsoftware.mvvm.data.local.mapper

import com.recipesforsoftware.mvvm.data.local.entity.CachedArticleEntity
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.Source

/**
 * Maps a cached entity to the domain model. Optional display fields are passed
 * through without placeholder values.
 */
internal fun CachedArticleEntity.toDomain(): Article =
    Article(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = sourceOrNull(),
    )

private fun CachedArticleEntity.sourceOrNull(): Source? =
    if (sourceId != null || sourceName != null) {
        Source(id = sourceId, name = sourceName)
    } else {
        null
    }

/**
 * Maps a domain article to a cache entity. [country], [feed] and
 * [positionInFeed] are supplied by the repository because the domain model is
 * intentionally unaware of caching scope.
 */
internal fun Article.toEntity(
    country: String,
    feed: String,
    positionInFeed: Int,
): CachedArticleEntity =
    CachedArticleEntity(
        country = country,
        feed = feed,
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        sourceId = source?.id,
        sourceName = source?.name,
        positionInFeed = positionInFeed,
    )
