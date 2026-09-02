package pl.recipesforsoftware.signalbrief.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Collection

/**
 * Domain-facing contract for the user's saved-article collections.
 *
 * Collections are framework-independent domain values: no article membership,
 * persistence, or sync concerns belong to this boundary yet. Implementations
 * live in the data layer and assign each collection a stable, unique [Collection.id].
 *
 * Name rule: names are normalized by trimming leading and trailing whitespace.
 * A blank-only name is rejected; otherwise the normalized name is returned.
 * Renaming to a blank name is also rejected.
 */
interface CollectionsRepository {
    /**
     * Returns all collections.
     *
     * The returned [Flow] emits the current collections immediately and again
     * on every create, rename, or delete.
     */
    fun observeAllCollections(): Flow<List<Collection>>

    /**
     * Creates a collection with the given [name].
     *
     * The name is normalized (trimmed) and a blank-only name is rejected with
     * [CollectionFailure.InvalidName]. On success the created [Collection] is
     * returned with its repository-assigned unique id and normalized name.
     */
    suspend fun createCollection(name: String): Result<Collection>

    /**
     * Renames the collection with the given [id] to [newName].
     *
     * The new name is normalized (trimmed) and renaming to a blank-only name is
     * rejected with [CollectionFailure.InvalidName]. A missing [id] fails with
     * [CollectionFailure.NotFound]. On success the renamed [Collection] is
     * returned.
     */
    suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection>

    /**
     * Deletes the collection with the given [id].
     *
     * A missing [id] fails with [CollectionFailure.NotFound]. On success the
     * collection is removed from [observeAllCollections].
     */
    suspend fun deleteCollection(id: String): Result<Unit>
}
