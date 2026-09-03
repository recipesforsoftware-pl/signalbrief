package pl.recipesforsoftware.signalbrief.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createTestDatabase
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behavioral tests for [RoomCollectionsRepository] against the real Room
 * database (in-memory on the JVM host, temporary file on the iOS simulator).
 * Verifies create/rename/delete semantics, deterministic newest-first ordering,
 * typed failure handling, and cancellation preservation.
 */
class RoomCollectionsRepositoryTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var repository: RoomCollectionsRepository
    private var clockCounter = 1000L

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        repository =
            RoomCollectionsRepository(
                database = database,
                clock = { clockCounter },
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun createCollection_validName_persistsNormalized() =
        runTest {
            clockCounter = 1000L
            val result = repository.createCollection("  Reading  ")

            assertTrue(result.isSuccess)
            assertEquals("Reading", result.getOrNull()?.name)
            assertEquals("1", result.getOrNull()?.id)

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals(1, collections.size)
                assertEquals("Reading", collections.single().name)
                assertEquals("1", collections.single().id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createCollection_blankName_returnsInvalidName() =
        runTest {
            val result = repository.createCollection("   ")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.InvalidName>(result.exceptionOrNull())

            repository.observeAllCollections().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createCollection_emptyName_returnsInvalidName() =
        runTest {
            val result = repository.createCollection("")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.InvalidName>(result.exceptionOrNull())
        }

    @Test
    fun multipleCreates_generateStableUniqueIds() =
        runTest {
            clockCounter = 1000L
            val first = repository.createCollection("First")
            clockCounter = 2000L
            val second = repository.createCollection("Second")
            clockCounter = 3000L
            val third = repository.createCollection("Third")

            assertTrue(first.isSuccess)
            assertTrue(second.isSuccess)
            assertTrue(third.isSuccess)

            val firstId = first.getOrNull()!!.id
            val secondId = second.getOrNull()!!.id
            val thirdId = third.getOrNull()!!.id

            assertTrue(firstId != secondId)
            assertTrue(secondId != thirdId)
            assertTrue(firstId != thirdId)
        }

    @Test
    fun observeAllCollections_emitsPersistedCollections() =
        runTest {
            clockCounter = 1000L
            repository.createCollection("Reading")

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals(1, collections.size)
                assertEquals("Reading", collections.single().name)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllCollections_newestCreatedFirst() =
        runTest {
            clockCounter = 1000L
            repository.createCollection("First")
            clockCounter = 3000L
            repository.createCollection("Second")
            clockCounter = 2000L
            repository.createCollection("Third")

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals(3, collections.size)
                assertEquals(
                    listOf("Second", "Third", "First"),
                    collections.map { it.name },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllCollections_equalTimestamp_orderedByIdDesc() =
        runTest {
            clockCounter = 1000L
            repository.createCollection("First")
            repository.createCollection("Second")
            repository.createCollection("Third")

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals(3, collections.size)
                assertEquals(
                    listOf("3", "2", "1"),
                    collections.map { it.id },
                )
                assertEquals(
                    listOf("Third", "Second", "First"),
                    collections.map { it.name },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeAllCollections_reflectsCreateRenameDelete() =
        runTest {
            repository.observeAllCollections().test {
                assertEquals(emptyList(), awaitItem())

                clockCounter = 1000L
                val created = repository.createCollection("Reading").getOrThrow()
                assertEquals(1, awaitItem().size)

                repository.renameCollection(created.id, "To Read")
                awaitItem().let { items ->
                    assertEquals(1, items.size)
                    assertEquals("To Read", items.single().name)
                }

                repository.deleteCollection(created.id)
                assertEquals(emptyList(), awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun renameCollection_persistsNormalizedName() =
        runTest {
            clockCounter = 1000L
            val created = repository.createCollection("Reading").getOrThrow()

            val renamed = repository.renameCollection(created.id, "  To Read  ")

            assertTrue(renamed.isSuccess)
            assertEquals("To Read", renamed.getOrNull()?.name)

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals("To Read", collections.single().name)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun renameCollection_doesNotChangeCreationOrder() =
        runTest {
            clockCounter = 1000L
            val first = repository.createCollection("First").getOrThrow()
            clockCounter = 2000L
            repository.createCollection("Second").getOrThrow()

            repository.renameCollection(first.id, "Renamed First")

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals(2, collections.size)
                assertEquals(
                    listOf("Second", "Renamed First"),
                    collections.map { it.name },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun renameCollection_blankName_returnsInvalidName() =
        runTest {
            clockCounter = 1000L
            val created = repository.createCollection("Reading").getOrThrow()

            val result = repository.renameCollection(created.id, "   ")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.InvalidName>(result.exceptionOrNull())

            repository.observeAllCollections().test {
                val collections = awaitItem()
                assertEquals("Reading", collections.single().name)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun renameCollection_missingId_returnsNotFound() =
        runTest {
            val result = repository.renameCollection("999", "New Name")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun renameCollection_nonNumericId_returnsNotFound() =
        runTest {
            val result = repository.renameCollection("not-a-number", "New Name")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun deleteCollection_removesCollection() =
        runTest {
            clockCounter = 1000L
            val created = repository.createCollection("Reading").getOrThrow()

            val result = repository.deleteCollection(created.id)

            assertTrue(result.isSuccess)

            repository.observeAllCollections().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deleteCollection_missingId_returnsNotFound() =
        runTest {
            val result = repository.deleteCollection("999")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun deleteCollection_nonNumericId_returnsNotFound() =
        runTest {
            val result = repository.deleteCollection("not-a-number")

            assertTrue(result.isFailure)
            assertIs<CollectionFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun createAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.createCollection("Reading")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }

    @Test
    fun renameAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.renameCollection("1", "New Name")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }

    @Test
    fun deleteAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.deleteCollection("1")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }
}
