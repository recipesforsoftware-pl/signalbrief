package pl.recipesforsoftware.signalbrief.data.local.mapper

import pl.recipesforsoftware.signalbrief.data.local.entity.MonitoredTopicEntity
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

/**
 * Maps a monitored-topic entity to the domain model. The auto-generated row id
 * is converted to a stable string identifier for domain consumption.
 */
internal fun MonitoredTopicEntity.toDomain(): MonitoredTopic =
    MonitoredTopic(
        id = id.toString(),
        query = query,
    )

/**
 * Creates a new [MonitoredTopicEntity] for insert. The [id] is left at zero so
 * Room auto-generates it.
 */
internal fun newMonitoredTopicEntity(
    query: String,
    normalizedQuery: String,
): MonitoredTopicEntity =
    MonitoredTopicEntity(
        id = 0,
        query = query,
        normalizedQuery = normalizedQuery,
    )
