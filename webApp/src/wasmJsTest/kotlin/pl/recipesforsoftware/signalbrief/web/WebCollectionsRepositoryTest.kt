package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebCollectionsRepositoryTest {
    @Test
    fun emptyStorageRestoresEmptyState() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            assertEquals(emptyList(), repository.observeAllCollections().first())
        }

    @Test
    fun missingStorageKeyRestoresEmptyState() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)

            assertTrue(repository.createCollection("Reading").isSuccess)
            val storage2 = FakeCollectionsStorage()
            val repository2 = WebCollectionsRepository(storage2)

            assertEquals(emptyList(), repository2.observeAllCollections().first())
        }

    @Test
    fun createCollectionPersistsNormalizedName() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            val result = repository.createCollection("  Reading  ")

            assertTrue(result.isSuccess)
            assertEquals("Reading", result.getOrNull()?.name)
        }

    @Test
    fun blankCreateReturnsInvalidNameAndDoesNotWrite() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)

            val result = repository.createCollection("   ")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.InvalidName>(result.exceptionOrNull())
            assertEquals(emptyList(), repository.observeAllCollections().first())
            assertTrue(storage.values.isEmpty())
        }

    @Test
    fun multipleCreatesProduceUniqueStableIds() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            val first = repository.createCollection("Reading")
            val second = repository.createCollection("Watchlist")

            assertTrue(first.isSuccess)
            assertTrue(second.isSuccess)
            assertTrue(first.getOrNull()!!.id != second.getOrNull()!!.id)
        }

    @Test
    fun newestCreatedFirstOrdering() =
        runTest {
            val clock = FakeClock()
            val repository = WebCollectionsRepository(FakeCollectionsStorage(), clock = clock::now)

            repository.createCollection("Reading")
            clock.advance(CLOCK_INCREMENT)
            repository.createCollection("Watchlist")

            val names = repository.observeAllCollections().first().map { it.name }
            assertEquals(listOf("Watchlist", "Reading"), names)
        }

    @Test
    fun deterministicTieBehaviorOnCreationTime() =
        runTest {
            val clock = FakeClock(1000L)
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage, clock = clock::now)

            repository.createCollection("First")
            repository.createCollection("Second")

            val collections = repository.observeAllCollections().first()
            assertEquals("Second", collections[0].name)
            assertEquals("First", collections[1].name)

            val reloaded = WebCollectionsRepository(storage, clock = clock::now)
            val reloadedCollections = reloaded.observeAllCollections().first()
            assertEquals("Second", reloadedCollections[0].name)
            assertEquals("First", reloadedCollections[1].name)
        }

    @Test
    fun reloadPreservesIdsNamesAndOrder() =
        runTest {
            val clock = FakeClock()
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage, clock = clock::now)

            repository.createCollection("Reading")
            clock.advance(CLOCK_INCREMENT)
            repository.createCollection("Watchlist")

            val original = repository.observeAllCollections().first()

            val restored = WebCollectionsRepository(storage, clock = clock::now)
            val restoredCollections = restored.observeAllCollections().first()

            assertEquals(original, restoredCollections)
        }

    @Test
    fun renamePersistsNormalizedName() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val created = repository.createCollection("Reading").getOrNull()!!

            val result = repository.renameCollection(created.id, "  Books  ")

            assertTrue(result.isSuccess)
            assertEquals("Books", result.getOrNull()?.name)
        }

    @Test
    fun renameDoesNotChangeCreationOrder() =
        runTest {
            val clock = FakeClock()
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage, clock = clock::now)

            repository.createCollection("Reading")
            clock.advance(CLOCK_INCREMENT)
            val watchlist = repository.createCollection("Watchlist").getOrNull()!!

            repository.renameCollection(watchlist.id, "Movies")

            val names = repository.observeAllCollections().first().map { it.name }
            assertEquals(listOf("Movies", "Reading"), names)
        }

    @Test
    fun observationReflectsCreateRenameDelete() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())

            repository.createCollection("Reading")
            assertEquals(1, repository.observeAllCollections().first().size)

            val collections = repository.observeAllCollections()
            val created = collections.first().first()
            repository.renameCollection(created.id, "Books")
            val renamed = repository.observeAllCollections()
            assertEquals("Books", renamed.first().first().name)

            repository.deleteCollection(created.id)
            assertEquals(emptyList(), repository.observeAllCollections().first())
        }

    private companion object {
        const val CLOCK_INCREMENT = 100L
    }
}
