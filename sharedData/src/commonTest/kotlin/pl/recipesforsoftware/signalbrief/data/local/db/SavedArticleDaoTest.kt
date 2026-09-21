package pl.recipesforsoftware.signalbrief.data.local.db

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.mapper.toDomain
import pl.recipesforsoftware.signalbrief.data.local.mapper.toEntity
import pl.recipesforsoftware.signalbrief.data.local.mapper.toSavedEntity
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedArticleDaoTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var dao: pl.recipesforsoftware.signalbrief.data.local.dao.SavedArticleDao

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        dao = database.savedArticleDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndRead_returnsSameArticle() =
        runTest {
            val article = sampleArticle(url = "https://example.com/1")

            dao.insertOrUpdate(article.toSavedEntity(savedAt = 1000L))

            val saved = dao.getAllOnce()
            assertEquals(1, saved.size)
            assertEquals(article, saved.single().toDomain())
        }

    @Test
    fun repeatedSave_doesNotCreateDuplicate() =
        runTest {
            val article = sampleArticle(url = "https://example.com/1")

            dao.insertOrUpdate(article.toSavedEntity(savedAt = 1000L))
            dao.insertOrUpdate(article.toSavedEntity(savedAt = 2000L))

            val saved = dao.getAllOnce()
            assertEquals(1, saved.size)
            assertEquals(2000L, saved.single().savedAt)
        }

    @Test
    fun removeByUrl_removesOnlyThatArticle() =
        runTest {
            val first = sampleArticle(url = "https://example.com/1")
            val second = sampleArticle(url = "https://example.com/2")

            dao.insertOrUpdate(first.toSavedEntity(savedAt = 1000L))
            dao.insertOrUpdate(second.toSavedEntity(savedAt = 2000L))
            dao.deleteByUrl("https://example.com/1")

            val saved = dao.getAllOnce()
            assertEquals(1, saved.size)
            assertEquals("https://example.com/2", saved.single().url)
        }

    @Test
    fun removeByNonexistentUrl_isNoOp() =
        runTest {
            val article = sampleArticle(url = "https://example.com/1")
            dao.insertOrUpdate(article.toSavedEntity(savedAt = 1000L))

            dao.deleteByUrl("https://example.com/nonexistent")

            assertEquals(1, dao.getAllOnce().size)
        }

    @Test
    fun multipleSaves_orderedBySavedAtDesc() =
        runTest {
            val first = sampleArticle(url = "https://example.com/1")
            val second = sampleArticle(url = "https://example.com/2")
            val third = sampleArticle(url = "https://example.com/3")

            dao.insertOrUpdate(first.toSavedEntity(savedAt = 1000L))
            dao.insertOrUpdate(second.toSavedEntity(savedAt = 3000L))
            dao.insertOrUpdate(third.toSavedEntity(savedAt = 2000L))

            val urls = dao.getAllOnce().map { it.url }
            assertEquals(
                listOf("https://example.com/2", "https://example.com/3", "https://example.com/1"),
                urls,
            )
        }

    @Test
    fun nullableFields_roundTripSafely() =
        runTest {
            val article =
                Article(
                    title = null,
                    description = null,
                    url = "https://example.com/minimal",
                    imageUrl = null,
                    source = null,
                )

            dao.insertOrUpdate(article.toSavedEntity(savedAt = 1000L))

            val saved = dao.getAllOnce().single().toDomain()
            assertNull(saved.title)
            assertNull(saved.description)
            assertNull(saved.imageUrl)
            assertNull(saved.source)
            assertEquals("https://example.com/minimal", saved.url)
        }

    @Test
    fun observeAll_emitsCurrentAndUpdates() =
        runTest {
            dao.observeAll().test {
                // Initial emission is an empty list
                assertEquals(emptyList(), awaitItem())

                dao.insertOrUpdate(sampleArticle(url = "https://example.com/1").toSavedEntity(savedAt = 1000L))
                val afterFirst = awaitItem()
                assertEquals(1, afterFirst.size)

                dao.insertOrUpdate(sampleArticle(url = "https://example.com/2").toSavedEntity(savedAt = 2000L))
                val afterSecond = awaitItem()
                assertEquals(2, afterSecond.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeByUrl_emitsNullWhenNotSaved() =
        runTest {
            dao.observeByUrl("https://example.com/1").test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun savedData_independentOfCacheTable() =
        runTest {
            val article = sampleArticle(url = "https://example.com/1")

            // Save to both tables
            dao.insertOrUpdate(article.toSavedEntity(savedAt = 1000L))
            database.articleDao().replaceAll(
                "us",
                "top-headlines",
                listOf(article.toEntity("us", "top-headlines", 0)),
            )

            // Clear the cache table
            database.articleDao().clearAll()

            // Saved article should still exist
            val saved = dao.getAllOnce()
            assertEquals(1, saved.size)
            assertEquals("https://example.com/1", saved.single().url)
        }

    @Test
    fun clearAll_removesAllSavedArticles() =
        runTest {
            dao.insertOrUpdate(sampleArticle(url = "https://example.com/1").toSavedEntity(savedAt = 1000L))
            dao.insertOrUpdate(sampleArticle(url = "https://example.com/2").toSavedEntity(savedAt = 2000L))

            dao.clearAll()

            assertTrue(dao.getAllOnce().isEmpty())
        }

    private companion object {
        fun sampleArticle(
            url: String,
            title: String = "Title for $url",
        ): Article =
            Article(
                title = title,
                description = "Description for $url",
                url = url,
                imageUrl = "https://example.com/image.jpg",
                source = Source(id = "source", name = "Example Source"),
            )
    }
}
