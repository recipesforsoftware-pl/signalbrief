package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebSavedArticlesRepositoryTest {
    @Test
    fun savedRepositorySavesReplacesAndRemovesArticles() =
        runTest {
            val repository = WebSavedArticlesRepository()
            val first = article(id = "first")
            val second = article(id = "second")

            assertTrue(repository.saveArticle(first).isSuccess)
            assertTrue(repository.saveArticle(second).isSuccess)

            assertEquals(
                listOf(second, first),
                repository.observeAllSavedArticles().first(),
            )
            assertTrue(repository.isArticleSaved(first.url).first())

            assertTrue(repository.saveArticle(first).isSuccess)

            assertEquals(
                listOf(first, second),
                repository.observeAllSavedArticles().first(),
            )

            assertTrue(repository.removeSavedArticle(first.url).isSuccess)

            assertEquals(
                listOf(second),
                repository.observeAllSavedArticles().first(),
            )
            assertFalse(repository.isArticleSaved(first.url).first())
        }

    private fun article(id: String): Article =
        Article(
            title = "Headline $id",
            description = "Description $id",
            url = "https://example.com/$id",
            imageUrl = "https://example.com/$id.jpg",
            source = Source(id = id, name = "Source $id"),
        )
}
