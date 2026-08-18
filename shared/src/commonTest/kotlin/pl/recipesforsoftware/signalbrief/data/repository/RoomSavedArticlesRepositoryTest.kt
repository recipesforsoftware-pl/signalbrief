package pl.recipesforsoftware.signalbrief.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behavioral tests for [RoomSavedArticlesRepository] against the real Room
 * database (in-memory on the JVM host, temporary file on the iOS simulator).
 * Verifies save/remove semantics, deterministic ordering, snapshot round-tripping,
 * cache independence, and typed failure handling.
 */
class RoomSavedArticlesRepositoryTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var repository: RoomSavedArticlesRepository
    private var clockCounter = 1000L

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        repository =
            RoomSavedArticlesRepository(
                database = database,
                clock = { clockCounter },
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveArticle_appearsInAllSaved() =
        runTest {
            val article = article("https://example.com/1")

            assertTrue(repository.saveArticle(article).isSuccess)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(1, saved.size)
                assertEquals(article, saved.first())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedSave_doesNotCreateDuplicate() =
        runTest {
            val article = article("https://example.com/1")

            clockCounter = 1000L
            assertTrue(repository.saveArticle(article).isSuccess)
            clockCounter = 2000L
            assertTrue(repository.saveArticle(article).isSuccess)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(1, saved.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedSave_refreshesMetadataAndSavedAt() =
        runTest {
            val original = article("https://example.com/1")
            clockCounter = 1000L
            repository.saveArticle(original)

            val updated =
                Article(
                    title = "Updated Title",
                    description = "Updated Description",
                    url = "https://example.com/1",
                    imageUrl = "https://example.com/new-image.jpg",
                    source = Source(id = "new-source", name = "New Source"),
                )
            clockCounter = 5000L
            repository.saveArticle(updated)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(1, saved.size)
                assertEquals("Updated Title", saved.first().title)
                assertEquals("Updated Description", saved.first().description)
                assertEquals("https://example.com/new-image.jpg", saved.first().imageUrl)
                assertEquals(Source(id = "new-source", name = "New Source"), saved.first().source)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun repeatedSave_movesItemToTop() =
        runTest {
            val first = article("https://example.com/1")
            val second = article("https://example.com/2")

            clockCounter = 1000L
            repository.saveArticle(first)
            clockCounter = 2000L
            repository.saveArticle(second)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(
                    listOf("https://example.com/2", "https://example.com/1"),
                    saved.map { it.url },
                )

                clockCounter = 3000L
                repository.saveArticle(first)

                val reordered = awaitItem()
                assertEquals(2, reordered.size)
                assertEquals(
                    listOf("https://example.com/1", "https://example.com/2"),
                    reordered.map { it.url },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun removeSavedArticle_disappears() =
        runTest {
            val article = article("https://example.com/1")
            repository.saveArticle(article)

            assertTrue(repository.removeSavedArticle("https://example.com/1").isSuccess)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertTrue(saved.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun removeByNonexistentUrl_isNoOp() =
        runTest {
            val article = article("https://example.com/1")
            repository.saveArticle(article)

            assertTrue(repository.removeSavedArticle("https://example.com/nonexistent").isSuccess)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(1, saved.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun multipleSaves_orderedBySavedAtDesc() =
        runTest {
            val first = article("https://example.com/1")
            val second = article("https://example.com/2")
            val third = article("https://example.com/3")

            clockCounter = 1000L
            repository.saveArticle(first)
            clockCounter = 3000L
            repository.saveArticle(second)
            clockCounter = 2000L
            repository.saveArticle(third)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(3, saved.size)
                assertEquals(
                    listOf("https://example.com/2", "https://example.com/3", "https://example.com/1"),
                    saved.map { it.url },
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun isArticleSaved_reflectsSaveAndRemove() =
        runTest {
            val article = article("https://example.com/1")

            repository.isArticleSaved("https://example.com/1").test {
                assertFalse(awaitItem())
                repository.saveArticle(article)
                assertTrue(awaitItem())
                repository.removeSavedArticle("https://example.com/1")
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun nullableFields_roundTripSafely() =
        runTest {
            val minimal =
                Article(
                    title = null,
                    description = null,
                    url = "https://example.com/minimal",
                    imageUrl = null,
                    source = null,
                )

            repository.saveArticle(minimal)

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(listOf(minimal), saved)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun savedData_independentOfCacheRefresh() =
        runTest {
            val article = article("https://example.com/1")
            repository.saveArticle(article)

            // Simulate cache refresh: replace the cache table contents
            // The saved table should be unaffected
            val cacheDao = database.articleDao()
            cacheDao.replaceAll(
                "us",
                "top-headlines",
                listOf(
                    pl.recipesforsoftware.signalbrief.data.local.entity.CachedArticleEntity(
                        country = "us",
                        feed = "top-headlines",
                        url = "https://example.com/other",
                        title = "Other",
                        description = null,
                        imageUrl = null,
                        sourceId = null,
                        sourceName = null,
                        positionInFeed = 0,
                    ),
                ),
            )
            cacheDao.clearAll()

            repository.observeAllSavedArticles().test {
                val saved = awaitItem()
                assertEquals(1, saved.size)
                assertEquals("https://example.com/1", saved.first().url)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun saveAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.saveArticle(article("https://example.com/1"))
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }

    @Test
    fun removeAfterDatabaseClose_throwsCancellationException() {
        database.close()

        runTest {
            try {
                repository.removeSavedArticle("https://example.com/1")
            } catch (_: CancellationException) {
                return@runTest
            }
            throw AssertionError("Expected CancellationException from closed database")
        }
    }

    @Test
    fun saveArticle_blankUrl_returnsInvalidData() =
        runTest {
            val article =
                Article(
                    title = "Title",
                    description = "Description",
                    url = "",
                    imageUrl = null,
                    source = null,
                )

            val result = repository.saveArticle(article)

            assertTrue(result.isFailure)
            assertIs<NewsFailure.InvalidData>(result.exceptionOrNull())

            repository.observeAllSavedArticles().test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun saveArticle_whitespaceUrl_returnsInvalidData() =
        runTest {
            val article =
                Article(
                    title = "Title",
                    description = "Description",
                    url = "   ",
                    imageUrl = null,
                    source = null,
                )

            val result = repository.saveArticle(article)

            assertTrue(result.isFailure)
            assertIs<NewsFailure.InvalidData>(result.exceptionOrNull())
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
