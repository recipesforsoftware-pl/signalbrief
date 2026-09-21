package pl.recipesforsoftware.signalbrief.data.local.mapper

import pl.recipesforsoftware.signalbrief.data.local.entity.SavedArticleEntity
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedArticleMapperTest {
    @Test
    fun entityToDomain_mapsAllFields() {
        val entity =
            SavedArticleEntity(
                url = "https://example.com/article",
                title = "Title",
                description = "Description",
                imageUrl = "https://example.com/image.jpg",
                sourceId = "source-id",
                sourceName = "Source Name",
                savedAt = 1000L,
            )

        val article = entity.toDomain()

        assertEquals("Title", article.title)
        assertEquals("Description", article.description)
        assertEquals("https://example.com/article", article.url)
        assertEquals("https://example.com/image.jpg", article.imageUrl)
        assertEquals(Source(id = "source-id", name = "Source Name"), article.source)
    }

    @Test
    fun entityToDomain_nullOptionalFields() {
        val entity =
            SavedArticleEntity(
                url = "https://example.com/article",
                title = null,
                description = null,
                imageUrl = null,
                sourceId = null,
                sourceName = null,
                savedAt = 1000L,
            )

        val article = entity.toDomain()

        assertNull(article.title)
        assertNull(article.description)
        assertNull(article.imageUrl)
        assertNull(article.source)
    }

    @Test
    fun domainToSavedEntity_mapsAllFields() {
        val article =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/article",
                imageUrl = "https://example.com/image.jpg",
                source = Source(id = "source-id", name = "Source Name"),
            )

        val entity = article.toSavedEntity(savedAt = 5000L)

        assertEquals("https://example.com/article", entity.url)
        assertEquals("Title", entity.title)
        assertEquals("Description", entity.description)
        assertEquals("https://example.com/image.jpg", entity.imageUrl)
        assertEquals("source-id", entity.sourceId)
        assertEquals("Source Name", entity.sourceName)
        assertEquals(5000L, entity.savedAt)
    }

    @Test
    fun roundTrip_preservesAllFields() {
        val original =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/article",
                imageUrl = "https://example.com/image.jpg",
                source = Source(id = "source-id", name = "Source Name"),
            )

        val entity = original.toSavedEntity(savedAt = 7000L)
        val roundTripped = entity.toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun roundTrip_withNullFields() {
        val original =
            Article(
                title = null,
                description = null,
                url = "https://example.com/article",
                imageUrl = null,
                source = null,
            )

        val entity = original.toSavedEntity(savedAt = 7000L)
        val roundTripped = entity.toDomain()

        assertEquals(original, roundTripped)
    }

    @Test
    fun roundTrip_withNullSourceId() {
        val original =
            Article(
                title = "Title",
                description = null,
                url = "https://example.com/article",
                imageUrl = null,
                source = Source(id = null, name = "Named Source"),
            )

        val entity = original.toSavedEntity(savedAt = 7000L)
        val roundTripped = entity.toDomain()

        assertEquals(original, roundTripped)
        assertEquals(Source(id = null, name = "Named Source"), roundTripped.source)
    }
}
