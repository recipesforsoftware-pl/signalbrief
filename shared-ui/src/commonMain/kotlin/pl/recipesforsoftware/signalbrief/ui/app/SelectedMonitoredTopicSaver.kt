package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic

/** Compose [Saver] for the selected Monitored Topic Matches destination. */
internal val SelectedMonitoredTopicSaver: Saver<MonitoredTopic?, Any> =
    listSaver(
        save = { topic -> topic?.let { listOf(it.id, it.query) }.orEmpty() },
        restore = { values ->
            values.takeIf { it.size == 2 }?.let {
                MonitoredTopic(id = it[0], query = it[1])
            }
        },
    )
