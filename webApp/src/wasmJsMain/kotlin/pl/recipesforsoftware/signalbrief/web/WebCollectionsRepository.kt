@file:OptIn(ExperimentalWasmJsInterop::class)

package pl.recipesforsoftware.signalbrief.web

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
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
    private var memberships = storage.restoreMemberships(persisted)
    private val membershipState = MutableStateFlow(memberships)

    private var nextId = restoredState.nextId

    override fun observeAllCollections(): Flow<List<Collection>> = collections

    override fun observeCollectionIdsForArticle(articleId: String): Flow<Set<String>> =
        membershipState.map { persistedMemberships ->
            persistedMemberships
                .asSequence()
                .filter { it.articleId == articleId }
                .map { it.collectionId.toString() }
                .toSet()
        }

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
        return deleteCommittedCollection(rowId)
    }

    /**
     * Durably deletes the collection identified by [rowId] and best-effort
     * cleans up its memberships.
     *
     * Collection state is authoritative: the collection deletion is persisted
     * first, so if that write fails the deletion is reported as failed and
     * neither observable state changes. Once committed, the logical in-memory
     * collections and memberships move to the deleted state and the membership
     * cleanup is best-effort: a failed membership write does NOT roll the
     * collection back because the authoritative collection no longer exists.
     * Stale orphan rows stay filtered out of the observable state, are ignored
     * during restore (see [restoreMemberships]) and are cleared by the next
     * successful membership write.
     */
    private suspend fun deleteCommittedCollection(rowId: Long): Result<Unit> {
        val candidate = persisted.filter { it.id != rowId }.sortedStable()
        val membershipCandidate = memberships.filter { it.collectionId != rowId }

        val deletionResult = persist(candidate, nextId)
        if (deletionResult.isFailure) {
            return deletionResult
        }

        persisted = candidate
        memberships = membershipCandidate
        collections.value = candidate.map(PersistedCollection::toDomain)
        membershipState.value = membershipCandidate

        storage.persistMemberships(membershipCandidate)
        return Result.success(Unit)
    }

    override suspend fun addArticleToCollection(
        article: Article,
        collectionId: String,
    ): Result<Unit> {
        val rowId = persisted.membershipCollectionId(article.url, collectionId).getOrElse { return Result.failure(it) }
        val candidate = (memberships + article.toMembership(rowId)).distinctBy { it.collectionId to it.articleId }
        return storage.persistMemberships(candidate).onSuccess {
            memberships = candidate
            membershipState.value = candidate
        }
    }

    override suspend fun removeArticleFromCollection(
        articleId: String,
        collectionId: String,
    ): Result<Unit> {
        val rowId = persisted.membershipCollectionId(articleId, collectionId).getOrElse { return Result.failure(it) }
        val candidate = memberships.filterNot { it.collectionId == rowId && it.articleId == articleId }
        return storage.persistMemberships(candidate).onSuccess {
            memberships = candidate
            membershipState.value = candidate
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

private object CollectionMembershipsCodec {
    private val json = Json

    fun encode(memberships: List<PersistedMembership>): String = json.encodeToString(memberships)

    fun decode(payload: String): List<PersistedMembership> = json.decodeFromString(payload)
}

private fun CollectionsStorage.restoreMemberships(collections: List<PersistedCollection>): List<PersistedMembership> {
    val validCollectionIds = collections.map(PersistedCollection::id).toSet()
    return runCatching {
        val stored =
            read(COLLECTION_MEMBERSHIPS_STORAGE_KEY)
                ?.let(CollectionMembershipsCodec::decode)
                .orEmpty()
        val cleaned =
            stored
                .filter { it.articleId.isNotBlank() && it.collectionId in validCollectionIds }
                .distinctBy { it.collectionId to it.articleId }
        if (cleaned != stored) {
            // Best-effort stale-orphan cleanup: the constructible view is
            // already filtered, so rewriting drops rows whose collection no
            // longer exists. Failures are swallowed so construction never
            // throws.
            persistMemberships(cleaned)
        }
        cleaned
    }.getOrDefault(emptyList())
}

private fun CollectionsStorage.persistMemberships(memberships: List<PersistedMembership>): Result<Unit> =
    runCatching {
        write(
            COLLECTION_MEMBERSHIPS_STORAGE_KEY,
            CollectionMembershipsCodec.encode(memberships),
        )
    }

private fun List<PersistedCollection>.membershipCollectionId(
    articleId: String,
    collectionId: String,
): Result<Long> =
    when {
        articleId.isBlank() -> {
            Result.failure(CollectionFailure.InvalidArticleId)
        }

        else -> {
            collectionId
                .toLongOrNull()
                ?.takeIf { rowId -> any { it.id == rowId } }
                ?.let(Result.Companion::success)
                ?: Result.failure(CollectionFailure.NotFound)
        }
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

@Serializable
internal data class PersistedMembership(
    val collectionId: Long,
    val articleId: String,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val source: SavedSource? = null,
)

/**
 * Maps a domain article into a persisted membership. The article URL is the
 * canonical identity; the display fields form a durable snapshot (the same
 * fields used by saved articles) so the membership stays renderable even after
 * the article is unsaved.
 */
private fun Article.toMembership(collectionId: Long): PersistedMembership =
    PersistedMembership(
        collectionId = collectionId,
        articleId = url,
        title = title,
        description = description,
        imageUrl = imageUrl,
        source = source?.toSavedSource(),
    )

internal const val COLLECTIONS_STORAGE_KEY = "signalbrief.collections.v1"
internal const val COLLECTION_MEMBERSHIPS_STORAGE_KEY = "signalbrief.collection-memberships.v1"

@Suppress("FunctionNaming")
private fun currentTimeMillis(): Long = dateNow.now().toLong()

private external interface DateJs : kotlin.js.JsAny {
    @JsName("now")
    fun now(): Double
}

private val dateNow: DateJs = js("Date")
