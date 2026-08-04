package com.recipesforsoftware.mvvm.data.remote.mapper

import com.recipesforsoftware.mvvm.data.remote.dto.ArticleDto
import com.recipesforsoftware.mvvm.data.remote.dto.SourceDto
import com.recipesforsoftware.mvvm.data.remote.dto.TopHeadlinesResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleMapperTest {
    @Test
    fun `maps a complete dto to domain`() {
        // Given
        val dto =
            ArticleDto(
                title = "Title",
                description = "Description",
                url = "https://example.com/a",
                imageUrl = "https://example.com/a.jpg",
                source = SourceDto(id = "src", name = "Source"),
            )

        // When
        val article = dto.toDomain()

        // Then
        assertEquals("Title", article?.title)
        assertEquals("Description", article?.description)
        assertEquals("https://example.com/a", article?.url)
        assertEquals("https://example.com/a.jpg", article?.imageUrl)
        assertEquals("src", article?.source?.id)
        assertEquals("Source", article?.source?.name)
    }

    @Test
    fun `keeps null optional fields as null`() {
        // Given
        val dto =
            ArticleDto(
                title = null,
                description = null,
                url = "https://example.com/a",
                imageUrl = null,
                source = null,
            )

        // When
        val article = dto.toDomain()

        // Then - nulls are preserved, no placeholder values are invented
        assertNull(article?.title)
        assertNull(article?.description)
        assertNull(article?.imageUrl)
        assertNull(article?.source)
    }

    @Test
    fun `returns null when url is missing`() {
        // Given
        val dto = ArticleDto(title = "Title", url = null)

        // When
        val article = dto.toDomain()

        // Then
        assertNull(article)
    }

    @Test
    fun `returns null when url is blank`() {
        // Given
        val dto = ArticleDto(title = "Title", url = "   ")

        // When
        val article = dto.toDomain()

        // Then
        assertNull(article)
    }

    @Test
    fun `maps a response and skips invalid articles`() {
        // Given
        val response =
            TopHeadlinesResponseDto(
                status = "ok",
                totalResults = 3,
                articles =
                    listOf(
                        ArticleDto(title = "A", url = "https://example.com/a"),
                        ArticleDto(title = "B", url = null),
                        ArticleDto(title = "C", url = "https://example.com/c"),
                    ),
            )

        // When
        val articles = response.toDomainArticles()

        // Then
        assertEquals(listOf("A", "C"), articles.map { it.title })
    }

    @Test
    fun `maps a null articles list to an empty list`() {
        // Given
        val response = TopHeadlinesResponseDto(status = "ok", totalResults = 0, articles = null)

        // When
        val articles = response.toDomainArticles()

        // Then
        assertTrue(articles.isEmpty())
    }

    @Test
    fun `maps a source with null fields`() {
        // Given
        val dto = SourceDto(id = null, name = null)

        // When
        val source = dto.toDomain()

        // Then
        assertNull(source.id)
        assertNull(source.name)
    }
}
