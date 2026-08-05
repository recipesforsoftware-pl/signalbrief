package pl.recipesforsoftware.signalbrief.data.local

import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createTestDatabase
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behavioral tests for [RoomNewsLocalDataSource] against the real Room database
 * (in-memory on the JVM host, temporary file on the iOS simulator). Verifies
 * domain-model round-tripping, stable ordering, country isolation and the typed
 * handling of database failures.
 */
class RoomNewsLocalDataSourceTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var dataSource: RoomNewsLocalDataSource

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        dataSource = RoomNewsLocalDataSource(database)
    }

    @AfterTest
    fun tearDown() {
        closeDatabaseQuietly()
    }

    /**
     * Closes the database defensively: on some hosts Room reports an
     * already-cancelled internal query job from `close()`. The typed failure
     * behavior under test comes from the subsequent DAO call, not from `close()`
     * itself.
     */
    @Suppress("SwallowedException")
    private fun closeDatabaseQuietly() {
        try {
            database.close()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            // Room host-specific close quirk; the assertion below still applies.
        }
    }

    @Test
    fun saveAndReadRoundTripsDomainModelsInStableOrder() =
        runTest {
            val articles =
                listOf(
                    article("https://example.com/1"),
                    article("https://example.com/2"),
                    article("https://example.com/3"),
                )

            assertTrue(dataSource.saveTopHeadlines("us", articles).isSuccess)

            val cached = dataSource.getTopHeadlines("us")
            assertTrue(cached.isSuccess)
            assertEquals(articles, cached.getOrNull())
        }

    @Test
    fun savingAgainReplacesThePreviousCountryCache() =
        runTest {
            dataSource.saveTopHeadlines("us", listOf(article("https://example.com/old")))
            dataSource.saveTopHeadlines("us", listOf(article("https://example.com/new")))

            val cachedUrls = dataSource.getTopHeadlines("us").getOrNull()?.map { it.url }
            assertEquals(listOf("https://example.com/new"), cachedUrls)
        }

    @Test
    fun savingAnEmptyListClearsThatCountry() =
        runTest {
            dataSource.saveTopHeadlines("us", listOf(article("https://example.com/1")))

            assertTrue(dataSource.saveTopHeadlines("us", emptyList()).isSuccess)

            assertTrue(dataSource.getTopHeadlines("us").getOrNull()?.isEmpty() == true)
        }

    @Test
    fun countryCachesRemainIsolated() =
        runTest {
            dataSource.saveTopHeadlines("us", listOf(article("https://example.com/us")))
            dataSource.saveTopHeadlines("pl", listOf(article("https://example.com/pl")))

            assertEquals(
                listOf("https://example.com/us"),
                dataSource.getTopHeadlines("us").getOrNull()?.map { it.url },
            )
            assertEquals(
                listOf("https://example.com/pl"),
                dataSource.getTopHeadlines("pl").getOrNull()?.map { it.url },
            )
            assertTrue(dataSource.getTopHeadlines("de").getOrNull()?.isEmpty() == true)
        }

    @Test
    fun nullableFieldsRoundTripSafely() =
        runTest {
            val minimal =
                Article(
                    title = null,
                    description = null,
                    url = "https://example.com/minimal",
                    imageUrl = null,
                    source = null,
                )

            dataSource.saveTopHeadlines("us", listOf(minimal))

            assertEquals(listOf(minimal), dataSource.getTopHeadlines("us").getOrNull())
        }

    @Test
    fun readAfterDatabaseCloseReturnsATypedFailure() =
        runTest {
            closeDatabaseQuietly()

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertIs<NewsFailure.Unknown>(result.exceptionOrNull())
        }

    @Test
    fun saveAfterDatabaseCloseReturnsATypedFailure() =
        runTest {
            closeDatabaseQuietly()

            val result = dataSource.saveTopHeadlines("us", listOf(article("https://example.com/1")))

            assertTrue(result.isFailure)
            assertIs<NewsFailure.Unknown>(result.exceptionOrNull())
        }

    private companion object {
        private val source = Source(id = "test", name = "Test News")

        fun article(url: String): Article =
            Article(
                title = "Headline for $url",
                description = "Description for $url",
                url = url,
                imageUrl = "https://example.com/image.jpg",
                source = source,
            )
    }
}
