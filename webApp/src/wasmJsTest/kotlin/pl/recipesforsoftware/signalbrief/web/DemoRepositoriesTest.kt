package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoRepositoriesTest {
    @Test
    fun demoFeedHasDeterministicOrderedContent() =
        runTest {
            val feed = DemoNewsRepository().getTopHeadlines(country = "us").getOrThrow()

            assertEquals(demoArticles, feed.articles)
            assertEquals(
                listOf(
                    "roof-garden",
                    "repair-cafe",
                    "night-buses",
                    "shade-trees",
                    "radio-archive",
                    "bookshops",
                    "river-insects",
                    "rehearsal",
                    "crossings",
                    "kitchens",
                ),
                feed.articles.map { it.source?.id },
            )
        }

    @Test
    fun savedRepositorySavesReplacesAndRemovesArticles() =
        runTest {
            val repository = WebSavedArticlesRepository()
            val first = demoArticles.first()
            val second = demoArticles[1]

            assertTrue(repository.saveArticle(first).isSuccess)
            assertTrue(repository.saveArticle(second).isSuccess)
            assertEquals(listOf(second, first), repository.observeAllSavedArticles().first())
            assertTrue(repository.isArticleSaved(first.url).first())

            assertTrue(repository.saveArticle(first).isSuccess)
            assertEquals(listOf(first, second), repository.observeAllSavedArticles().first())

            assertTrue(repository.removeSavedArticle(first.url).isSuccess)
            assertEquals(listOf(second), repository.observeAllSavedArticles().first())
            assertFalse(repository.isArticleSaved(first.url).first())
        }
}
