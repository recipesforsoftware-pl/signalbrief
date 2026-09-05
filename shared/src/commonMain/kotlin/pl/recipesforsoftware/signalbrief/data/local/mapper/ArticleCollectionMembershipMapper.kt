package pl.recipesforsoftware.signalbrief.data.local.mapper

import pl.recipesforsoftware.signalbrief.data.local.entity.ArticleCollectionMembershipEntity
import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Maps a domain article into a collection-membership entity.
 *
 * The article URL becomes the [ArticleCollectionMembershipEntity.articleId]
 * identity and a snapshot of the article's durable display fields (the same
 * fields persisted by saved articles) is stored alongside it so the membership
 * stays renderable after the article is unsaved.
 */
internal fun Article.toMembershipEntity(collectionId: Long): ArticleCollectionMembershipEntity =
    ArticleCollectionMembershipEntity(
        collectionId = collectionId,
        articleId = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        sourceId = source?.id,
        sourceName = source?.name,
    )
