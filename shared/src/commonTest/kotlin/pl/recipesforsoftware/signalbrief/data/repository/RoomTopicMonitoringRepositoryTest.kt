package pl.recipesforsoftware.signalbrief.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createTestDatabase
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behavioral tests for [RoomTopicMonitoringRepository] against the real Room
 * database (in-memory on the JVM host, temporary file on the iOS simulator).
 * Verifies create/update/delete semantics, deterministic ordering, typed
 * failure handling, persistence across re-reads, and cancellation preservation.
 */
class RoomTopicMonitoringRepositoryTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var repository: RoomTopicMonitoringRepository

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        repository = RoomTopicMonitoringRepository(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeTopics_emptyInitialState() =
        runTest {
            repository.observeTopics().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createTopic_validQuery_persistsNormalizedQuery() =
        runTest {
            val result = repository.createTopic("  Kotlin Multiplatform  ")

            assertTrue(result.isSuccess)
            assertEquals("Kotlin Multiplatform", result.getOrNull()?.query)
            assertEquals("1", result.getOrNull()?.id)

            repository.observeTopics().test {
                val topics = awaitItem()
                assertEquals(1, topics.size)
                assertEquals("Kotlin Multiplatform", topics.single().query)
                assertEquals("1", topics.single().id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createTopic_blankQuery_returnsInvalidQuery() =
        runTest {
            val result = repository.createTopic("   ")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.InvalidQuery>(result.exceptionOrNull())

            repository.observeTopics().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createTopic_duplicateQueryCaseInsensitive_returnsDuplicateQuery() =
        runTest {
            assertTrue(repository.createTopic("Kotlin").isSuccess)

            val result = repository.createTopic("kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.DuplicateQuery>(result.exceptionOrNull())

            repository.observeTopics().test {
                assertEquals(1, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createTopics_emitDeterministicOrdering() =
        runTest {
            repository.createTopic("Zebra")
            repository.createTopic("apple")
            repository.createTopic("Market")

            repository.observeTopics().test {
                val topics = awaitItem()
                assertEquals(3, topics.size)
                assertEquals(
                    listOf("apple", "Market", "Zebra"),
                    topics.map { it.query },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateTopic_persistsChangedQuery() =
        runTest {
            val created = repository.createTopic("Kotlin").getOrThrow()

            val result = repository.updateTopic(created.id, "  Swift  ")

            assertTrue(result.isSuccess)
            assertEquals("Swift", result.getOrNull()?.query)

            repository.observeTopics().test {
                val topics = awaitItem()
                assertEquals(1, topics.size)
                assertEquals("Swift", topics.single().query)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateTopic_equivalentOwnQuery_succeeds() =
        runTest {
            val created = repository.createTopic("Kotlin").getOrThrow()

            val result = repository.updateTopic(created.id, "kotlin")

            assertTrue(result.isSuccess)
            val updatedQuery =
                repository
                    .observeTopics()
                    .first()
                    .single()
                    .query
            assertEquals("kotlin", updatedQuery)
        }

    @Test
    fun updateTopic_equivalentOwnQueryWithCasingChange_succeedsAndUpdatesDisplayQuery() =
        runTest {
            val created = repository.createTopic("kotlin").getOrThrow()

            val result = repository.updateTopic(created.id, "Kotlin")

            assertTrue(result.isSuccess)
            val updatedQueries =
                repository
                    .observeTopics()
                    .first()
                    .map { it.query }
                    .toSet()
            assertEquals(setOf("Kotlin"), updatedQueries)
        }

    @Test
    fun updateTopic_anotherTopicsNormalizedQuery_returnsDuplicateQuery() =
        runTest {
            repository.createTopic("Kotlin").getOrThrow()
            val swift = repository.createTopic("Swift").getOrThrow()

            val result = repository.updateTopic(swift.id, "kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.DuplicateQuery>(result.exceptionOrNull())

            repository.observeTopics().test {
                val topics = awaitItem()
                assertEquals("Swift", topics.single { it.id == swift.id }.query)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun updateTopic_unknownId_returnsNotFound() =
        runTest {
            val result = repository.updateTopic("999", "Kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun updateTopic_nonNumericId_returnsNotFound() =
        runTest {
            val result = repository.updateTopic("not-a-number", "Kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun updateTopic_blankQuery_returnsInvalidQuery() =
        runTest {
            val created = repository.createTopic("Kotlin").getOrThrow()

            val result = repository.updateTopic(created.id, "   ")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.InvalidQuery>(result.exceptionOrNull())

            repository.observeTopics().test {
                assertEquals("Kotlin", awaitItem().single().query)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deleteTopic_emitsRemoval() =
        runTest {
            val created = repository.createTopic("Kotlin").getOrThrow()

            val result = repository.deleteTopic(created.id)

            assertTrue(result.isSuccess)

            repository.observeTopics().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deleteTopic_unknownId_returnsNotFound() =
        runTest {
            val result = repository.deleteTopic("999")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun deleteTopic_nonNumericId_returnsNotFound() =
        runTest {
            val result = repository.deleteTopic("not-a-number")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun observeTopics_reflectsCreateUpdateDelete() =
        runTest {
            repository.observeTopics().test {
                assertEquals(emptyList(), awaitItem())

                val created = repository.createTopic("Kotlin").getOrThrow()
                assertEquals(1, awaitItem().size)

                repository.updateTopic(created.id, "AI")
                awaitItem().let { topics ->
                    assertEquals(1, topics.size)
                    assertEquals("AI", topics.single().query)
                }

                repository.deleteTopic(created.id)
                assertEquals(emptyList(), awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeTopics_mapsLongIdsToDomainStrings() =
        runTest {
            repository.createTopic("First")
            repository.createTopic("Second")

            val topics = repository.observeTopics().first()
            assertTrue(topics.all { it.id.toLongOrNull() != null })
            assertEquals(
                listOf("1", "2"),
                topics.map { it.id },
            )
        }

    @Test
    fun persistence_survivesRepositoryRecreation() =
        runTest {
            repository.createTopic("Kotlin")
            repository.createTopic("Swift")

            val topicsBefore = repository.observeTopics().first()
            val newRepository = RoomTopicMonitoringRepository(database)

            assertEquals(topicsBefore, newRepository.observeTopics().first())
            assertEquals(2, newRepository.observeTopics().first().size)
        }

    @Test
    fun createAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.createTopic("Kotlin")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }

    @Test
    fun updateAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.updateTopic("1", "Swift")
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
                repository.deleteTopic("1")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }
}
