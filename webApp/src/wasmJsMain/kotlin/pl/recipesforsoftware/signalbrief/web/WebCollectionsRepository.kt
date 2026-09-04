@file:OptIn(ExperimentalWasmJsInterop::class)

package pl.recipesforsoftware.signalbrief.web

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.CollectionName
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

internal class WebCollectionsRepository(
    private val storage: CollectionsStorage = BrowserCollectionsStorage(),
    private val clock: () -> Long = { currentTimeMillis() },
    private val idGenerator: () -> Long = { 1L },
) : CollectionsRepository {
    private val restoredState = restorePersisted()
    private var persisted: List<PersistedCollection> = restoredState.collections
    private val collections = MutableStateFlow(persisted.map(PersistedCollection::toDomain))

    private var nextId = restoredState.nextId

    override fun observeAllCollections(): Flow<List<Collection>> = collections

    override suspend fun createCollection(name: String): Result<Collection> {
        val collectionName =
            CollectionName.from(name)
                ?: return Result.failure(CollectionFailure.InvalidName)
        val candidateId = nextId
        val entry =
            PersistedCollection(
                id = candidateId,
                name = collectionName.value,
                createdAt = clock(),
            )
        val candidate = (listOf(entry) + persisted).sortedStable()
        return persist(candidate, candidateId + 1)
            .onSuccess {
                persisted = candidate
                nextId = candidateId + 1
                collections.value = candidate.map(PersistedCollection::toDomain)
            }.map { entry.toDomain() }
    }

    override suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection> {
        val collectionName = CollectionName.from(newName)
        val rowId = id.toLongOrNull()
        val existing = rowId?.let { rid -> persisted.find { it.id == rid } }
        if (collectionName == null || existing == null) {
            return Result.failure(
                if (collectionName == null) CollectionFailure.InvalidName else CollectionFailure.NotFound,
            )
        }
        val renamed = existing.copy(name = collectionName.value)
        val candidate = persisted.map { if (it.id == existing.id) renamed else it }.sortedStable()
        return persist(candidate, nextId)
            .onSuccess {
                persisted = candidate
                collections.value = candidate.map(PersistedCollection::toDomain)
            }.map { renamed.toDomain() }
    }

    override suspend fun deleteCollection(id: String): Result<Unit> {
        val rowId = id.toLongOrNull()
        if (rowId == null || persisted.none { it.id == rowId }) {
            return Result.failure(CollectionFailure.NotFound)
        }
        val candidate = persisted.filter { it.id != rowId }.sortedStable()
        return persist(candidate, nextId)
            .onSuccess {
                persisted = candidate
                collections.value = candidate.map(PersistedCollection::toDomain)
            }
    }

    private fun restorePersisted(): PersistedCollectionsState =
        runCatching {
            val raw =
                storage
                    .read(COLLECTIONS_STORAGE_KEY)
                    ?.let(CollectionsCodec::decode)
                    ?: return@runCatching PersistedCollectionsState(nextId = idGenerator(), collections = emptyList())
            val sortedCollections = raw.collections.sortedStable()
            val maxId = sortedCollections.maxOfOrNull { it.id } ?: 0L
            PersistedCollectionsState(
                nextId = maxOf(raw.nextId, maxId + 1),
                collections = sortedCollections,
            )
        }.getOrElse { PersistedCollectionsState(nextId = idGenerator(), collections = emptyList()) }

    private fun persist(
        collections: List<PersistedCollection>,
        nextId: Long,
    ): Result<Unit> =
        runCatching {
            storage.write(
                COLLECTIONS_STORAGE_KEY,
                CollectionsCodec.encode(PersistedCollectionsState(nextId = nextId, collections = collections)),
            )
        }

    private fun List<PersistedCollection>.sortedStable(): List<PersistedCollection> =
        sortedWith(compareByDescending<PersistedCollection> { it.createdAt }.thenByDescending { it.id })
}

internal interface CollectionsStorage {
    fun read(key: String): String?

    fun write(
        key: String,
        value: String,
    )
}

private class BrowserCollectionsStorage : CollectionsStorage {
    override fun read(key: String): String? = window.localStorage.getItem(key)

    override fun write(
        key: String,
        value: String,
    ) {
        window.localStorage.setItem(key, value)
    }
}

private object CollectionsCodec {
    private val json = Json

    fun encode(state: PersistedCollectionsState): String = json.encodeToString(state)

    fun decode(payload: String): PersistedCollectionsState = json.decodeFromString(payload)
}

@Serializable
internal data class PersistedCollectionsState(
    val nextId: Long,
    val collections: List<PersistedCollection>,
)

@Serializable
internal data class PersistedCollection(
    val id: Long,
    val name: String,
    val createdAt: Long,
) {
    fun toDomain(): Collection =
        Collection(
            id = id.toString(),
            name = name,
        )
}

internal const val COLLECTIONS_STORAGE_KEY = "signalbrief.collections.v1"

@Suppress("FunctionNaming")
private fun currentTimeMillis(): Long = dateNow.now().toLong()

private external interface DateJs : kotlin.js.JsAny {
    @JsName("now")
    fun now(): Double
}

private val dateNow: DateJs = js("Date")
