package pl.recipesforsoftware.signalbrief.web

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.TopicQuery
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository

/**
 * localStorage-backed [TopicMonitoringRepository].
 *
 * The full monitored-topic list is the single source of truth under
 * [TOPIC_MONITORS_STORAGE_KEY]. Observable state only changes after a
 * successful storage write, reloading from the same storage reconstructs the
 * same topic list, and malformed payloads restore safely to an empty list.
 *
 * Semantics match the Room implementation: queries are trimmed and blank-only
 * queries are rejected; duplicate detection is case-insensitive on the trimmed
 * query; updating a topic to its own normalized query succeeds. Ordering is
 * deterministic: normalized query ascending, then id ascending.
 */
internal class WebTopicMonitoringRepository(
    private val storage: TopicMonitoringStorage = BrowserTopicMonitoringStorage(),
    private val idGenerator: () -> Long = { 1L },
) : TopicMonitoringRepository {
    private val restoredState = restorePersisted()
    private var persisted: List<PersistedMonitoredTopic> = restoredState.topics
    private val topics = MutableStateFlow(persisted.map(PersistedMonitoredTopic::toDomain))

    private var nextId = restoredState.nextId

    override fun observeTopics(): Flow<List<MonitoredTopic>> = topics

    override suspend fun createTopic(query: String): Result<MonitoredTopic> {
        val topicQuery =
            TopicQuery.from(query)
                ?: return Result.failure(TopicMonitoringFailure.InvalidQuery)
        return createValidated(topicQuery)
    }

    private suspend fun createValidated(topicQuery: TopicQuery): Result<MonitoredTopic> {
        if (persisted.any { it.normalizedQuery == topicQuery.normalizedKey }) {
            return Result.failure(TopicMonitoringFailure.DuplicateQuery)
        }
        val entry =
            PersistedMonitoredTopic(
                id = nextId,
                query = topicQuery.value,
                normalizedQuery = topicQuery.normalizedKey,
            )
        val candidate = (persisted + entry).sortedDeterministic()
        return persist(PersistedTopicMonitorsState(nextId = nextId + 1, topics = candidate))
            .onSuccess {
                persisted = candidate
                nextId = nextId + 1
                topics.value = candidate.map(PersistedMonitoredTopic::toDomain)
            }.map { entry.toDomain() }
    }

    override suspend fun updateTopic(
        id: String,
        query: String,
    ): Result<MonitoredTopic> {
        val topicQuery = TopicQuery.from(query)
        val rowId = id.toLongOrNull()
        val existing = rowId?.let { rid -> persisted.find { it.id == rid } }
        if (topicQuery == null || existing == null) {
            return Result.failure(
                if (topicQuery == null) TopicMonitoringFailure.InvalidQuery else TopicMonitoringFailure.NotFound,
            )
        }
        return updateValidated(
            topicQuery = topicQuery,
            rowId = requireNotNull(rowId),
            existing = existing,
        )
    }

    private suspend fun updateValidated(
        topicQuery: TopicQuery,
        rowId: Long,
        existing: PersistedMonitoredTopic,
    ): Result<MonitoredTopic> {
        if (persisted.any { it.normalizedQuery == topicQuery.normalizedKey && it.id != rowId }) {
            return Result.failure(TopicMonitoringFailure.DuplicateQuery)
        }
        val updated = existing.copy(query = topicQuery.value, normalizedQuery = topicQuery.normalizedKey)
        val candidate = persisted.map { if (it.id == rowId) updated else it }.sortedDeterministic()
        return persist(PersistedTopicMonitorsState(nextId = nextId, topics = candidate))
            .onSuccess {
                persisted = candidate
                topics.value = candidate.map(PersistedMonitoredTopic::toDomain)
            }.map { updated.toDomain() }
    }

    override suspend fun deleteTopic(id: String): Result<Unit> {
        val rowId = id.toLongOrNull()
        if (rowId == null || persisted.none { it.id == rowId }) {
            return Result.failure(TopicMonitoringFailure.NotFound)
        }
        val candidate = persisted.filter { it.id != rowId }.sortedDeterministic()
        return persist(PersistedTopicMonitorsState(nextId = nextId, topics = candidate))
            .onSuccess {
                persisted = candidate
                topics.value = candidate.map(PersistedMonitoredTopic::toDomain)
            }
    }

    private fun restorePersisted(): PersistedTopicMonitorsState =
        runCatching {
            val raw =
                storage
                    .read(TOPIC_MONITORS_STORAGE_KEY)
                    ?.let(TopicMonitorsCodec::decode)
                    ?: return@runCatching PersistedTopicMonitorsState(nextId = idGenerator(), topics = emptyList())
            val sortedTopics = raw.topics.sortedDeterministic()
            val maxId = sortedTopics.maxOfOrNull { it.id } ?: 0L
            PersistedTopicMonitorsState(
                nextId = maxOf(raw.nextId, maxId + 1),
                topics = sortedTopics,
            )
        }.getOrElse { PersistedTopicMonitorsState(nextId = idGenerator(), topics = emptyList()) }

    private fun persist(state: PersistedTopicMonitorsState): Result<Unit> =
        runCatching {
            storage.write(TOPIC_MONITORS_STORAGE_KEY, TopicMonitorsCodec.encode(state))
        }
}

internal interface TopicMonitoringStorage {
    fun read(key: String): String?

    fun write(
        key: String,
        value: String,
    )
}

private class BrowserTopicMonitoringStorage : TopicMonitoringStorage {
    override fun read(key: String): String? = window.localStorage.getItem(key)

    override fun write(
        key: String,
        value: String,
    ) {
        window.localStorage.setItem(key, value)
    }
}

private object TopicMonitorsCodec {
    private val json = Json

    fun encode(state: PersistedTopicMonitorsState): String = json.encodeToString(state)

    fun decode(payload: String): PersistedTopicMonitorsState = json.decodeFromString(payload)
}

/** Cross-platform deterministic ordering: normalized query ascending, id ascending. */
private fun List<PersistedMonitoredTopic>.sortedDeterministic(): List<PersistedMonitoredTopic> =
    sortedWith(compareBy<PersistedMonitoredTopic> { it.normalizedQuery }.thenBy { it.id })

@Serializable
internal data class PersistedTopicMonitorsState(
    val nextId: Long,
    val topics: List<PersistedMonitoredTopic>,
)

@Serializable
internal data class PersistedMonitoredTopic(
    val id: Long,
    val query: String,
    val normalizedQuery: String,
) {
    fun toDomain(): MonitoredTopic =
        MonitoredTopic(
            id = id.toString(),
            query = query,
        )
}

internal const val TOPIC_MONITORS_STORAGE_KEY = "signalbrief.topic-monitors.v1"
