package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebCollectionsRepositoryFailureTest {
    @Test
    fun blankRenameReturnsInvalidNameWithoutMutation() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val created = repository.createCollection("Reading").getOrNull()!!
            val originalState = repository.observeAllCollections().first()

            val result = repository.renameCollection(created.id, "   ")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.InvalidName>(result.exceptionOrNull())
            assertEquals(originalState, repository.observeAllCollections().first())
        }

    @Test
    fun renameMissingIdReturnsNotFound() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            val result = repository.renameCollection("999", "New Name")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun deletePersistsRemoval() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val created = repository.createCollection("Reading").getOrNull()!!

            assertTrue(repository.deleteCollection(created.id).isSuccess)
            assertEquals(emptyList(), repository.observeAllCollections().first())
        }

    @Test
    fun deleteMissingIdReturnsNotFound() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            val result = repository.deleteCollection("999")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun malformedJsonRestoresSafelyToEmpty() =
        runTest {
            val storage =
                FakeCollectionsStorage(
                    values = mutableMapOf(COLLECTIONS_STORAGE_KEY to "not valid json"),
                )

            assertEquals(emptyList(), WebCollectionsRepository(storage).observeAllCollections().first())
        }

    @Test
    fun storageReadFailureRestoresSafelyToEmpty() =
        runTest {
            val storage = FakeCollectionsStorage(readFailure = IllegalStateException("read failed"))

            assertEquals(emptyList(), WebCollectionsRepository(storage).observeAllCollections().first())
        }

    @Test
    fun storageWriteFailureReturnsFailureAndLeavesObservableStateUnchanged() =
        runTest {
            val storage = FakeCollectionsStorage(writeFailure = IllegalStateException("write failed"))
            val repository = WebCollectionsRepository(storage)

            val result = repository.createCollection("Reading")

            assertTrue(result.isFailure)
            assertEquals(emptyList(), repository.observeAllCollections().first())
        }

    @Test
    fun failedRenameWriteLeavesOldNameAndStateIntact() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val created = repository.createCollection("Reading").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")

            val result = repository.renameCollection(created.id, "Books")

            assertTrue(result.isFailure)
            assertEquals(
                listOf(created),
                repository.observeAllCollections().first(),
            )
        }

    @Test
    fun failedDeleteWriteLeavesItemAndStateIntact() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val created = repository.createCollection("Reading").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")

            val result = repository.deleteCollection(created.id)

            assertTrue(result.isFailure)
            assertEquals(
                listOf(created),
                repository.observeAllCollections().first(),
            )
        }

    @Test
    fun failedCreateWriteDoesNotConsumeNextId() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val first = repository.createCollection("First").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")
            repository.createCollection("Second")

            storage.writeFailure = null
            val third = repository.createCollection("Third").getOrNull()!!

            assertEquals(first.id.toLong() + 1, third.id.toLong())
        }

    @Test
    fun deletedCollectionIdIsNotReusedAfterReload() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val a = repository.createCollection("A").getOrNull()!!

            repository.deleteCollection(a.id)
            val reloaded = WebCollectionsRepository(storage)
            val b = reloaded.createCollection("B").getOrNull()!!

            assertTrue(a.id.toLong() < b.id.toLong())
        }
}

internal class FakeCollectionsStorage(
    val values: MutableMap<String, String> = mutableMapOf(),
    private val readFailure: Exception? = null,
    var writeFailure: Exception? = null,
) : CollectionsStorage {
    override fun read(key: String): String? {
        if (readFailure != null) {
            throw readFailure
        }
        return values[key]
    }

    override fun write(
        key: String,
        value: String,
    ) {
        if (writeFailure != null) {
            throw writeFailure!!
        }
        values[key] = value
    }
}

internal class FakeClock(
    private var time: Long = 0L,
) {
    fun now(): Long = time

    fun advance(millis: Long) {
        time += millis
    }
}
