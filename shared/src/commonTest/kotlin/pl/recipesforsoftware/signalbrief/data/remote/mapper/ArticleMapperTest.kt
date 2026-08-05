package pl.recipesforsoftware.signalbrief.data.remote.mapper

import pl.recipesforsoftware.signalbrief.data.remote.dto.ArticleDto
import pl.recipesforsoftware.signalbrief.data.remote.dto.SourceDto
import pl.recipesforsoftware.signalbrief.data.remote.dto.TopHeadlinesResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleMapperTest {
    @Test
    fun mapsACompleteDtoToDomain() {
        val dto =
            ArticleDto(
                title = "Title",
                description = "Description",
                url = "https://example.com/a",
                imageUrl = "https://example.com/a.jpg",
                source = SourceDto(id = "src", name = "Source"),
            )

        val article = dto.toDomain()

        assertEquals("Title", article?.title)
        assertEquals("Description", article?.description)
        assertEquals("https://example.com/a", article?.url)
        assertEquals("https://example.com/a.jpg", article?.imageUrl)
        assertEquals("src", article?.source?.id)
        assertEquals("Source", article?.source?.name)
    }

    @Test
    fun keepsNullOptionalFieldsAsNull() {
        val dto =
            ArticleDto(
                title = null,
                description = null,
                url = "https://example.com/a",
                imageUrl = null,
                source = null,
            )

        val article = dto.toDomain()

        assertNull(article?.title)
        assertNull(article?.description)
        assertNull(article?.imageUrl)
        assertNull(article?.source)
    }

    @Test
    fun returnsNullWhenUrlIsMissing() {
        val dto = ArticleDto(title = "Title", url = null)

        val article = dto.toDomain()

        assertNull(article)
    }

    @Test
    fun returnsNullWhenUrlIsBlank() {
        val dto = ArticleDto(title = "Title", url = "   ")

        val article = dto.toDomain()

        assertNull(article)
    }

    @Test
    fun mapsAResponseAndSkipsInvalidArticles() {
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

        val articles = response.toDomainArticles()

        assertEquals(listOf("A", "C"), articles.map { it.title })
    }

    @Test
    fun mapsANullArticlesListToAnEmptyList() {
        val response = TopHeadlinesResponseDto(status = "ok", totalResults = 0, articles = null)

        val articles = response.toDomainArticles()

        assertTrue(articles.isEmpty())
    }

    @Test
    fun mapsASourceWithNullFields() {
        val dto = SourceDto(id = null, name = null)

        val source = dto.toDomain()

        assertNull(source.id)
        assertNull(source.name)
    }
}
