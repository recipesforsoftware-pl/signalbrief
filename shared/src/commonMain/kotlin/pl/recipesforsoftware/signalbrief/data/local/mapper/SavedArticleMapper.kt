package pl.recipesforsoftware.signalbrief.data.local.mapper

import pl.recipesforsoftware.signalbrief.data.local.entity.SavedArticleEntity
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source

/**
 * Maps a saved-article entity to the domain model. Optional display fields are
 * passed through without placeholder values.
 */
internal fun SavedArticleEntity.toDomain(): Article =
    Article(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = sourceOrNull(),
    )

private fun SavedArticleEntity.sourceOrNull(): Source? =
    if (sourceId != null || sourceName != null) {
        Source(id = sourceId, name = sourceName)
    } else {
        null
    }

/**
 * Maps a domain article to a saved-article entity. [savedAt] is supplied by the
 * caller because the domain model is intentionally unaware of persistence
 * concerns.
 */
internal fun Article.toSavedEntity(savedAt: Long): SavedArticleEntity =
    SavedArticleEntity(
        url = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        sourceId = source?.id,
        sourceName = source?.name,
        savedAt = savedAt,
    )
