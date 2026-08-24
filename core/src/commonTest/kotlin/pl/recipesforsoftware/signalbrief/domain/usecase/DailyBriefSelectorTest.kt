package pl.recipesforsoftware.signalbrief.domain.usecase

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DailyBriefSelectorTest {
    private val selector = DailyBriefSelector()

    @Test
    fun `empty input produces an empty brief`() {
        assertEquals(emptyList(), selector(emptyList()).articles)
    }

    @Test
    fun `one eligible article produces a one article brief`() {
        val article = article(url = "https://example.com/one")

        assertEquals(listOf(article), selector(listOf(article)).articles)
    }

    @Test
    fun `selection preserves cached feed order`() {
        val first = article(url = "https://example.com/first")
        val second = article(url = "https://example.com/second")
        val third = article(url = "https://example.com/third")

        assertEquals(listOf(first, second, third), selector(listOf(first, second, third)).articles)
    }

    @Test
    fun `selection is capped at five articles`() {
        val articles = (1..6).map { article(url = "https://example.com/$it") }

        assertEquals(articles.take(5), selector(articles).articles)
    }

    @Test
    fun `blank and non actionable urls are removed`() {
        val valid = article(url = "https://example.com/valid")
        val articles =
            listOf(
                article(url = " "),
                article(url = "javascript:alert(1)"),
                article(url = "mailto:news@example.com"),
                article(url = "file:///private/article"),
                valid,
            )

        assertEquals(listOf(valid), selector(articles).articles)
    }

    @Test
    fun `duplicate urls retain the first cached article`() {
        val first = article(url = "https://example.com/story", title = "First")
        val duplicate = article(url = "https://example.com/story", title = "Duplicate")
        val second = article(url = "https://example.com/second")

        assertEquals(listOf(first, second), selector(listOf(first, duplicate, second)).articles)
    }

    @Test
    fun `repeated selection produces identical output`() {
        val articles =
            listOf(
                article(url = "https://example.com/first"),
                article(url = "https://example.com/first", title = "Duplicate"),
                article(url = "http://example.com/second"),
            )

        assertEquals(selector(articles), selector(articles))
    }

    @Test
    fun `selection preserves the original article fields`() {
        val source = Source(id = "publisher", name = "Publisher")
        val article =
            Article(
                title = "Original title",
                description = "Original description",
                url = "https://example.com/original",
                imageUrl = "https://example.com/original.png",
                source = source,
            )

        val selected = selector(listOf(article)).articles.single()

        assertSame(article, selected)
        assertEquals("Original title", selected.title)
        assertEquals("Original description", selected.description)
        assertEquals("https://example.com/original.png", selected.imageUrl)
        assertEquals(source, selected.source)
    }

    private fun article(
        url: String,
        title: String = "Title for $url",
    ): Article =
        Article(
            title = title,
            description = "Description for $url",
            url = url,
            imageUrl = "https://example.com/image.png",
            source = Source(id = "source", name = "Source"),
        )
}
