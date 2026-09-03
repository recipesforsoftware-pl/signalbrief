package pl.recipesforsoftware.signalbrief.data.local.mapper

import pl.recipesforsoftware.signalbrief.data.local.entity.CollectionEntity
import pl.recipesforsoftware.signalbrief.domain.model.Collection

/**
 * Maps a collection entity to the domain model. The auto-generated row id is
 * converted to a stable string identifier for domain consumption.
 */
internal fun CollectionEntity.toDomain(): Collection =
    Collection(
        id = id.toString(),
        name = name,
    )

/**
 * Creates a new [CollectionEntity] for insert. The [id] is left at zero so
 * Room auto-generates it.
 */
internal fun newCollectionEntity(
    name: String,
    createdAt: Long,
): CollectionEntity =
    CollectionEntity(
        id = 0,
        name = name,
        createdAt = createdAt,
    )
