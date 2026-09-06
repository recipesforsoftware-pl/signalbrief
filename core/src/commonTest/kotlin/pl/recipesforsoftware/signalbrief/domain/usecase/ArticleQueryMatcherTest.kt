package pl.recipesforsoftware.signalbrief.domain.usecase

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleQueryMatcherTest {
    private val exampleUrl = "https://example.com/story"

    private fun article(
        title: String? = "Kotlin Multiplatform reaches 1.0",
        description: String? = "The team ships a stable release.",
        url: String = exampleUrl,
        imageUrl: String? = "https://images.example.com/pic.jpg",
        source: Source? = Source(id = "src", name = "Example News"),
    ) = Article(title, description, url, imageUrl, source)

    @Test
    fun `matches title`() {
        assertTrue(ArticleQueryMatcher.matches(article(title = "Kotlin on the server"), "Kotlin"))
    }

    @Test
    fun `matches description`() {
        assertTrue(ArticleQueryMatcher.matches(article(description = "Learn Kotlin coroutines"), "coroutines"))
    }

    @Test
    fun `matches source name`() {
        assertTrue(ArticleQueryMatcher.matches(article(source = Source(null, "Reuters Finance")), "Reuters"))
    }

    @Test
    fun `match is case insensitive`() {
        assertTrue(ArticleQueryMatcher.matches(article(title = "KOTLIN"), "kotlin"))
        assertTrue(ArticleQueryMatcher.matches(article(title = "kotlin"), "KOTLIN"))
    }

    @Test
    fun `query is trimmed before matching`() {
        assertTrue(ArticleQueryMatcher.matches(article(title = "Kotlin"), "  Kotlin  "))
    }

    @Test
    fun `blank query matches nothing`() {
        assertFalse(ArticleQueryMatcher.matches(article(), ""))
        assertFalse(ArticleQueryMatcher.matches(article(), "   "))
    }

    @Test
    fun `query occurring only in url does not match`() {
        val a = article(title = "Generic headline", url = "https://example.com/story")
        assertFalse(ArticleQueryMatcher.matches(a, "example.com"))
    }

    @Test
    fun `query occurring only in image url does not match`() {
        val a = article(title = "Generic headline", imageUrl = "https://images.example.com/pic.jpg")
        assertFalse(ArticleQueryMatcher.matches(a, "images.example.com"))
    }

    @Test
    fun `filter preserves article input order`() {
        val first = article(title = "Kotlin first", url = "https://a.com/1")
        val second = article(title = "Swift second", url = "https://a.com/2")
        val third = article(title = "Kotlin third", url = "https://a.com/3")
        val result = ArticleQueryMatcher.filter(listOf(first, second, third), "kotlin")
        assertEquals(listOf(first, third), result)
    }

    @Test
    fun `no match across all fields returns false`() {
        val a =
            article(
                title = "Unrelated heading",
                description = "Unrelated body",
                source = Source(null, "Unrelated Source"),
            )
        assertFalse(ArticleQueryMatcher.matches(a, "missing"))
    }
}
