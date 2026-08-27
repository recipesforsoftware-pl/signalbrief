package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebSavedArticlesRepositoryTest {
    @Test
    fun savedRepositorySavesReplacesAndRemovesArticles() =
        runTest {
            val repository = WebSavedArticlesRepository(FakeSavedArticlesStorage())
            val first = article(id = "first")
            val second = article(id = "second")
            val refreshedFirst = first.copy(title = "Updated headline")

            assertTrue(repository.saveArticle(first).isSuccess)
            assertTrue(repository.saveArticle(second).isSuccess)

            assertEquals(
                listOf(second, first),
                repository.observeAllSavedArticles().first(),
            )
            assertTrue(repository.isArticleSaved(first.url).first())

            assertTrue(repository.saveArticle(refreshedFirst).isSuccess)

            assertEquals(
                listOf(refreshedFirst, second),
                repository.observeAllSavedArticles().first(),
            )

            assertTrue(repository.removeSavedArticle(refreshedFirst.url).isSuccess)

            assertEquals(
                listOf(second),
                repository.observeAllSavedArticles().first(),
            )
            assertFalse(repository.isArticleSaved(first.url).first())
        }

    @Test
    fun savedArticlesAreWrittenAndRestoredInTheirSavedOrder() =
        runTest {
            val storage = FakeSavedArticlesStorage()
            val first =
                article(id = "first").copy(
                    title = null,
                    description = null,
                    imageUrl = null,
                    source = Source(id = null, name = null),
                )
            val second = article(id = "second")
            val repository = WebSavedArticlesRepository(storage)

            assertTrue(repository.saveArticle(first).isSuccess)
            assertTrue(repository.saveArticle(second).isSuccess)
            assertTrue(storage.values.containsKey(SAVED_ARTICLES_STORAGE_KEY))

            val restoredRepository = WebSavedArticlesRepository(storage)

            assertEquals(
                listOf(second, first),
                restoredRepository.observeAllSavedArticles().first(),
            )
            assertTrue(restoredRepository.isArticleSaved(first.url).first())
        }

    @Test
    fun removingSavedArticlePersistsTheRemoval() =
        runTest {
            val storage = FakeSavedArticlesStorage()
            val first = article(id = "first")
            val second = article(id = "second")
            val repository = WebSavedArticlesRepository(storage)
            repository.saveArticle(first)
            repository.saveArticle(second)

            assertTrue(repository.removeSavedArticle(second.url).isSuccess)

            assertEquals(
                listOf(first),
                WebSavedArticlesRepository(storage).observeAllSavedArticles().first(),
            )
        }

    @Test
    fun malformedPersistedArticlesFallBackToAnEmptyList() =
        runTest {
            val storage =
                FakeSavedArticlesStorage(
                    values = mutableMapOf(SAVED_ARTICLES_STORAGE_KEY to "not valid json"),
                )

            assertEquals(
                emptyList(),
                WebSavedArticlesRepository(storage).observeAllSavedArticles().first(),
            )
        }

    @Test
    fun storageReadFailureFallsBackToAnEmptyList() =
        runTest {
            val storage = FakeSavedArticlesStorage(readFailure = IllegalStateException("read failed"))

            assertEquals(
                emptyList(),
                WebSavedArticlesRepository(storage).observeAllSavedArticles().first(),
            )
        }

    @Test
    fun storageWriteFailureReturnsFailureWithoutChangingSavedArticles() =
        runTest {
            val storage = FakeSavedArticlesStorage(writeFailure = IllegalStateException("write failed"))
            val repository = WebSavedArticlesRepository(storage)

            assertTrue(repository.saveArticle(article(id = "first")).isFailure)
            assertEquals(emptyList(), repository.observeAllSavedArticles().first())
            assertNull(storage.values[SAVED_ARTICLES_STORAGE_KEY])
        }

    private fun article(id: String): Article =
        Article(
            title = "Headline $id",
            description = "Description $id",
            url = "https://example.com/$id",
            imageUrl = "https://example.com/$id.jpg",
            source = Source(id = id, name = "Source $id"),
        )

    private class FakeSavedArticlesStorage(
        val values: MutableMap<String, String> = mutableMapOf(),
        private val readFailure: Exception? = null,
        private val writeFailure: Exception? = null,
    ) : SavedArticlesStorage {
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
                throw writeFailure
            }
            values[key] = value
        }
    }
}
