package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebTopicMonitoringRepositoryTest {
    @Test
    fun emptyStorageRestoresEmptyState() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            assertEquals(emptyList(), repository.observeTopics().first())
        }

    @Test
    fun missingStorageKeyRestoresEmptyState() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)

            assertTrue(repository.createTopic("Kotlin").isSuccess)
            val storage2 = FakeTopicMonitoringStorage()
            val repository2 = WebTopicMonitoringRepository(storage2)

            assertEquals(emptyList(), repository2.observeTopics().first())
        }

    @Test
    fun createTopicPersistsTrimmedQuery() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            val result = repository.createTopic("  Kotlin Multiplatform  ")

            assertTrue(result.isSuccess)
            assertEquals("Kotlin Multiplatform", result.getOrNull()?.query)
        }

    @Test
    fun createTopicsProduceUniqueStableIds() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            val first = repository.createTopic("Kotlin")
            val second = repository.createTopic("Swift")

            assertTrue(first.isSuccess)
            assertTrue(second.isSuccess)
            assertTrue(first.getOrNull()!!.id != second.getOrNull()!!.id)
        }

    @Test
    fun duplicateCreateIsRejectedCaseInsensitively() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            assertTrue(repository.createTopic("Kotlin").isSuccess)

            val result = repository.createTopic("kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.DuplicateQuery>(result.exceptionOrNull())
            assertEquals(1, repository.observeTopics().first().size)
        }

    @Test
    fun blankCreateReturnsInvalidQueryAndDoesNotWrite() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)

            val result = repository.createTopic("   ")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.InvalidQuery>(result.exceptionOrNull())
            assertEquals(emptyList(), repository.observeTopics().first())
            assertTrue(storage.values.isEmpty())
        }

    @Test
    fun deterministicQueryAscendingOrdering() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            repository.createTopic("Zebra")
            repository.createTopic("apple")
            repository.createTopic("Market")

            val queries = repository.observeTopics().first().map { it.query }
            assertEquals(listOf("apple", "Market", "Zebra"), queries)
        }

    @Test
    fun reloadPreservesIdsQueriesAndOrder() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)

            repository.createTopic("Zebra")
            repository.createTopic("apple")
            repository.createTopic("Market")

            val original = repository.observeTopics().first()

            val restored = WebTopicMonitoringRepository(storage)
            val restoredTopics = restored.observeTopics().first()

            assertEquals(original, restoredTopics)
        }

    @Test
    fun deleteTopicRemovesIt() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            val created = repository.createTopic("Kotlin").getOrNull()!!

            val result = repository.deleteTopic(created.id)

            assertTrue(result.isSuccess)
            assertEquals(emptyList(), repository.observeTopics().first())
        }

    @Test
    fun observationReflectsCreateUpdateDelete() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            assertEquals(emptyList(), repository.observeTopics().first())

            val created = repository.createTopic("Kotlin").getOrNull()!!
            assertEquals(1, repository.observeTopics().first().size)

            repository.updateTopic(created.id, "AI")
            assertEquals(listOf("AI"), repository.observeTopics().first().map { it.query })

            repository.deleteTopic(created.id)
            assertEquals(emptyList(), repository.observeTopics().first())
        }
}
