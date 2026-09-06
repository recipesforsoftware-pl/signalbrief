package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebTopicMonitoringRepositoryMutationTest {
    @Test
    fun updateTopicPersistsTrimmedQuery() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            val created = repository.createTopic("Kotlin").getOrNull()!!

            val result = repository.updateTopic(created.id, "  Swift  ")

            assertTrue(result.isSuccess)
            assertEquals("Swift", result.getOrNull()?.query)
            assertEquals(listOf("Swift"), repository.observeTopics().first().map { it.query })
        }

    @Test
    fun updateTopicEquivalentOwnQuerySucceeds() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            val created = repository.createTopic("Kotlin").getOrNull()!!

            val result = repository.updateTopic(created.id, "kotlin")

            assertTrue(result.isSuccess)
            assertEquals(listOf("kotlin"), repository.observeTopics().first().map { it.query })
        }

    @Test
    fun updateTopicEquivalentOwnQueryWithCasingChangeSucceedsAndUpdatesDisplayQuery() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            val created = repository.createTopic("kotlin").getOrNull()!!

            val result = repository.updateTopic(created.id, "Kotlin")

            assertTrue(result.isSuccess)
            assertEquals(listOf("Kotlin"), repository.observeTopics().first().map { it.query })
        }

    @Test
    fun updateTopicToAnotherExistingQueryIsRejected() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            repository.createTopic("Kotlin")
            val swift = repository.createTopic("Swift").getOrNull()!!

            val result = repository.updateTopic(swift.id, "kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.DuplicateQuery>(result.exceptionOrNull())
            val queries = repository.observeTopics().first().map { it.query }
            assertEquals(listOf("Kotlin", "Swift"), queries)
        }

    @Test
    fun updateUnknownIdReturnsNotFound() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            val result = repository.updateTopic("999", "Kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun updateNonNumericIdReturnsNotFound() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            val result = repository.updateTopic("not-a-number", "Kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }

    @Test
    fun blankUpdateReturnsInvalidQueryWithoutMutation() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())
            val created = repository.createTopic("Kotlin").getOrNull()!!

            val result = repository.updateTopic(created.id, "   ")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.InvalidQuery>(result.exceptionOrNull())
            assertEquals(listOf("Kotlin"), repository.observeTopics().first().map { it.query })
        }

    @Test
    fun deleteUnknownIdReturnsNotFound() =
        runTest {
            val repository = WebTopicMonitoringRepository(FakeTopicMonitoringStorage())

            val result = repository.deleteTopic("999")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.NotFound>(result.exceptionOrNull())
        }
}
