package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.failure.TopicMonitoringFailure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebTopicMonitoringRepositoryFailureTest {
    @Test
    fun malformedJsonRestoresSafelyToEmpty() =
        runTest {
            val storage =
                FakeTopicMonitoringStorage(
                    values = mutableMapOf(TOPIC_MONITORS_STORAGE_KEY to "not valid json"),
                )

            assertEquals(emptyList(), WebTopicMonitoringRepository(storage).observeTopics().first())
        }

    @Test
    fun storageReadFailureRestoresSafelyToEmpty() =
        runTest {
            val storage = FakeTopicMonitoringStorage(readFailure = IllegalStateException("read failed"))

            assertEquals(emptyList(), WebTopicMonitoringRepository(storage).observeTopics().first())
        }

    @Test
    fun storageWriteFailureReturnsFailureAndLeavesObservableStateUnchanged() =
        runTest {
            val storage = FakeTopicMonitoringStorage(writeFailure = IllegalStateException("write failed"))
            val repository = WebTopicMonitoringRepository(storage)

            val result = repository.createTopic("Kotlin")

            assertTrue(result.isFailure)
            assertEquals(emptyList(), repository.observeTopics().first())
        }

    @Test
    fun failedCreateWriteDoesNotConsumeNextId() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            val first = repository.createTopic("First").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")
            repository.createTopic("Second")

            storage.writeFailure = null
            val third = repository.createTopic("Third").getOrNull()!!

            assertEquals(first.id.toLong() + 1, third.id.toLong())
        }

    @Test
    fun failedUpdateWriteLeavesOldQueryAndStateIntact() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            val created = repository.createTopic("Kotlin").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")

            val result = repository.updateTopic(created.id, "Swift")

            assertTrue(result.isFailure)
            assertEquals(
                listOf(created),
                repository.observeTopics().first(),
            )
        }

    @Test
    fun failedDeleteWriteLeavesItemAndStateIntact() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            val created = repository.createTopic("Kotlin").getOrNull()!!

            storage.writeFailure = IllegalStateException("write failed")

            val result = repository.deleteTopic(created.id)

            assertTrue(result.isFailure)
            assertEquals(
                listOf(created),
                repository.observeTopics().first(),
            )
        }

    @Test
    fun duplicateCreateDoesNotWrite() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            repository.createTopic("Kotlin")

            val result = repository.createTopic("kotlin")

            assertTrue(result.isFailure)
            assertIs<TopicMonitoringFailure.DuplicateQuery>(result.exceptionOrNull())
            assertEquals(1, decodePersisted(storage).topics.size)
        }

    @Test
    fun deletedTopicIdIsNotReusedAfterReload() =
        runTest {
            val storage = FakeTopicMonitoringStorage()
            val repository = WebTopicMonitoringRepository(storage)
            val a = repository.createTopic("A").getOrNull()!!

            repository.deleteTopic(a.id)
            val reloaded = WebTopicMonitoringRepository(storage)
            val b = reloaded.createTopic("B").getOrNull()!!

            assertTrue(a.id.toLong() < b.id.toLong())
        }

    private fun decodePersisted(storage: FakeTopicMonitoringStorage): PersistedTopicMonitorsState {
        val payload = storage.values[TOPIC_MONITORS_STORAGE_KEY] ?: return PersistedTopicMonitorsState(0L, emptyList())
        return Json.decodeFromString(payload)
    }
}

/**
 * In-memory [TopicMonitoringStorage] test double.
 *
 * Writes can be forced to fail via [writeFailure] (fails every write) or
 * [failWriteNumbers] (fails the Nth write, 1-based across the lifetime of the
 * fake, see [writeCount]).
 */
internal class FakeTopicMonitoringStorage(
    val values: MutableMap<String, String> = mutableMapOf(),
    private val readFailure: Exception? = null,
    var writeFailure: Exception? = null,
    val failWriteNumbers: MutableSet<Int> = mutableSetOf(),
) : TopicMonitoringStorage {
    var writeCount: Int = 0

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
        writeCount++
        val failure =
            writeFailure
                ?: if (writeCount in failWriteNumbers) {
                    IllegalStateException("write failed at #$writeCount")
                } else {
                    null
                }
        if (failure != null) {
            throw failure
        }
        values[key] = value
    }
}
