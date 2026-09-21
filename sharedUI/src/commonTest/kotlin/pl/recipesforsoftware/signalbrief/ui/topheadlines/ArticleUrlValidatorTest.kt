package pl.recipesforsoftware.signalbrief.ui.topheadlines

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the article URL validation helper. These are pure domain/UI
 * boundary checks with no Compose runtime or platform dependencies.
 */
class ArticleUrlValidatorTest {
    @Test
    fun `https url is actionable`() {
        assertTrue(article(url = "https://example.com/story").hasActionableUrl())
    }

    @Test
    fun `http url is actionable`() {
        assertTrue(article(url = "http://example.com/story").hasActionableUrl())
    }

    @Test
    fun `mixed case scheme is actionable`() {
        assertTrue(article(url = "HTTPS://example.com/story").hasActionableUrl())
        assertTrue(article(url = "Http://example.com/story").hasActionableUrl())
    }

    @Test
    fun `non http scheme is not actionable`() {
        assertFalse(article(url = "javascript:alert(1)").hasActionableUrl())
        assertFalse(article(url = "file:///etc/passwd").hasActionableUrl())
        assertFalse(article(url = "mailto:test@example.com").hasActionableUrl())
    }

    @Test
    fun `blank url is not actionable`() {
        assertFalse(article(url = "   ").hasActionableUrl())
    }

    @Test
    fun `empty url is not actionable`() {
        assertFalse(article(url = "").hasActionableUrl())
    }

    private fun article(url: String): Article =
        Article(
            title = "Title",
            description = null,
            url = url,
            imageUrl = null,
            source = Source(id = "src", name = "Source"),
        )
}
