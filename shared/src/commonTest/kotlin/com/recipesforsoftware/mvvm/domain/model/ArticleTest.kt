package com.recipesforsoftware.mvvm.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ArticleTest {
    @Test
    fun articlesWithSameValuesAreEqual() {
        val first =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/a",
                imageUrl = "https://example.com/a.jpg",
                source = Source(id = "src", name = "Source"),
            )
        val second =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/a",
                imageUrl = "https://example.com/a.jpg",
                source = Source(id = "src", name = "Source"),
            )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun articlesDifferingInUrlAreNotEqual() {
        val first =
            Article(
                title = "Title",
                description = null,
                url = "https://example.com/a",
                imageUrl = null,
                source = null,
            )
        val second =
            Article(
                title = "Title",
                description = null,
                url = "https://example.com/b",
                imageUrl = null,
                source = null,
            )

        assertNotEquals(first, second)
    }

    @Test
    fun copyChangesOnlyTheRequestedField() {
        val article =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/a",
                imageUrl = "https://example.com/a.jpg",
                source = Source(id = "src", name = "Source"),
            )

        val copied = article.copy(title = "Updated")

        assertEquals("Updated", copied.title)
        assertEquals(article.description, copied.description)
        assertEquals(article.url, copied.url)
        assertEquals(article.imageUrl, copied.imageUrl)
        assertEquals(article.source, copied.source)
        assertNotEquals(article, copied)
    }

    @Test
    fun optionalFieldsMayBeNullWithoutBreakingTheValue() {
        val article =
            Article(
                title = null,
                description = null,
                url = "https://example.com/a",
                imageUrl = null,
                source = null,
            )
        assertNull(article.title)
        assertNull(article.description)
        assertNull(article.imageUrl)
        assertNull(article.source)
        assertEquals("https://example.com/a", article.url)
    }

    @Test
    fun sourcesAreComparedByTheirValues() {
        assertEquals(Source(id = "src", name = "Source"), Source(id = "src", name = "Source"))
        assertNotEquals(Source(id = "src", name = "Source"), Source(id = "other", name = "Source"))
        assertNotEquals(Source(id = null, name = "Source"), Source(id = "src", name = "Source"))
    }
}
