package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

/** KMP-safe [Saver] for Navigation 3 Topic Matches rendering payloads. */
internal val NavigationMonitoredTopicSnapshotsSaver: Saver<Map<String, MonitoredTopic>, Any> =
    listSaver(
        save = { snapshots -> snapshots.values.flatMap { listOf(it.id, it.query) } },
        restore = { values ->
            values
                .takeIf { it.size % MONITORED_TOPIC_SAVED_VALUE_COUNT == 0 }
                ?.chunked(MONITORED_TOPIC_SAVED_VALUE_COUNT)
                ?.mapNotNull { saved -> runCatching { MonitoredTopic(saved[0], saved[1]) }.getOrNull() }
                ?.associateBy(MonitoredTopic::id)
                .orEmpty()
        },
    )

private const val MONITORED_TOPIC_SAVED_VALUE_COUNT = 2
