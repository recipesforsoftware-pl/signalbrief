package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.Transactor
import androidx.room.useWriterConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.TOP_HEADLINES_FEED
import pl.recipesforsoftware.signalbrief.data.local.mapper.toDomain
import pl.recipesforsoftware.signalbrief.data.local.mapper.toEntity
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CachedArticleDaoTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var dao: pl.recipesforsoftware.signalbrief.data.local.dao.CachedArticleDao

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        dao = database.articleDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRead_oneCountry_returnsSameArticles() =
        runTest {
            val articles =
                listOf(
                    sampleArticle(url = "https://example.com/1", title = "First"),
                )

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            val cached = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED)
            assertEquals(1, cached.size)
            assertEquals(articles.single(), cached.single().toDomain())
        }

    @Test
    fun readOrder_matchesInsertOrder() =
        runTest {
            val articles =
                listOf(
                    sampleArticle(url = "https://example.com/1"),
                    sampleArticle(url = "https://example.com/2"),
                    sampleArticle(url = "https://example.com/3"),
                )

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            val cachedUrls = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).map { it.url }
            assertEquals(listOf("https://example.com/1", "https://example.com/2", "https://example.com/3"), cachedUrls)
        }

    @Test
    fun countryIsolation_readsOnlyRequestedCountry() =
        runTest {
            val usArticles = listOf(sampleArticle(url = "https://example.com/us"))
            val plArticles = listOf(sampleArticle(url = "https://example.com/pl"))

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, usArticles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))
            dao.replaceAll(COUNTRY_PL, TOP_HEADLINES_FEED, plArticles.toEntities(COUNTRY_PL, TOP_HEADLINES_FEED))

            assertEquals(1, dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).size)
            assertEquals(1, dao.getByCountryAndFeed(COUNTRY_PL, TOP_HEADLINES_FEED).size)
            assertTrue(dao.getByCountryAndFeed("de", TOP_HEADLINES_FEED).isEmpty())
        }

    @Test
    fun nullableFields_roundTripSafely() =
        runTest {
            val article =
                Article(
                    title = null,
                    description = null,
                    url = "https://example.com/minimal",
                    imageUrl = null,
                    source = null,
                )

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, listOf(article.toEntity(COUNTRY_US, TOP_HEADLINES_FEED, 0)))

            val cached = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).single().toDomain()
            assertNull(cached.title)
            assertNull(cached.description)
            assertNull(cached.imageUrl)
            assertNull(cached.source)
            assertEquals("https://example.com/minimal", cached.url)
        }

    @Test
    fun replaceAll_removesOldRowsForSameCountryAndFeed() =
        runTest {
            val first = listOf(sampleArticle(url = "https://example.com/old"))
            val second = listOf(sampleArticle(url = "https://example.com/new"))

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, first.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, second.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            val cachedUrls = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).map { it.url }
            assertEquals(listOf("https://example.com/new"), cachedUrls)
        }

    @Test
    fun replaceAll_withEmptyList_clearsThatCountry() =
        runTest {
            val articles = listOf(sampleArticle(url = "https://example.com/1"))

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, emptyList())

            assertTrue(dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).isEmpty())
        }

    @Test
    fun duplicateIdentity_doesNotCreateDuplicates() =
        runTest {
            val article = sampleArticle(url = "https://example.com/shared")

            dao.insertAll(article.toEntity(COUNTRY_US, TOP_HEADLINES_FEED, 0).let(::listOf))
            dao.insertAll(article.toEntity(COUNTRY_US, TOP_HEADLINES_FEED, 1).let(::listOf))

            val cached = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED)
            assertEquals(1, cached.size)
            assertEquals(1, cached.single().positionInFeed)
        }

    @Test
    fun entityToDomain_andDomainToEntity_areConsistent() {
        val original =
            Article(
                title = "Title",
                description = "Description",
                url = "https://example.com/article",
                imageUrl = "https://example.com/image.jpg",
                source = Source(id = "source-id", name = "Source Name"),
            )

        val entity = original.toEntity(COUNTRY_US, TOP_HEADLINES_FEED, 7)
        val roundTripped = entity.toDomain()

        assertEquals(original, roundTripped)
        assertEquals(COUNTRY_US, entity.country)
        assertEquals(TOP_HEADLINES_FEED, entity.feed)
        assertEquals(7, entity.positionInFeed)
        assertEquals("source-id", entity.sourceId)
        assertEquals("Source Name", entity.sourceName)
    }

    @Test
    fun sourceWithNullId_roundTripsToDomain() =
        runTest {
            val article =
                Article(
                    title = "Title",
                    description = null,
                    url = "https://example.com/article",
                    imageUrl = null,
                    source = Source(id = null, name = "Named Source"),
                )

            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, listOf(article.toEntity(COUNTRY_US, TOP_HEADLINES_FEED, 0)))

            val cached = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).single().toDomain()
            assertEquals(Source(id = null, name = "Named Source"), cached.source)
        }

    @Test
    fun transaction_rollsBackOnFailure() =
        runTest {
            val articles = listOf(sampleArticle(url = "https://example.com/rollback"))
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            assertFailsWith<IllegalStateException> {
                database.useWriterConnection { transactor ->
                    transactor.withTransaction(Transactor.SQLiteTransactionType.DEFERRED) {
                        dao.deleteByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED)
                        error("Simulated transaction failure")
                    }
                }
            }

            val cached = dao.getByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED)
            assertEquals(listOf("https://example.com/rollback"), cached.map { it.url })
        }

    @Test
    fun observeByCountryAndFeed_emitsCurrentArticles() =
        runTest {
            val articles = listOf(sampleArticle(url = "https://example.com/1"))
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            val observed = dao.observeByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).first()

            assertEquals(1, observed.size)
            assertEquals("https://example.com/1", observed.single().url)
        }

    @Test
    fun observeByCountryAndFeed_preservesInsertOrder() =
        runTest {
            val articles =
                listOf(
                    sampleArticle(url = "https://example.com/1"),
                    sampleArticle(url = "https://example.com/2"),
                    sampleArticle(url = "https://example.com/3"),
                )
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, articles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))

            val observed = dao.observeByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).first()

            assertEquals(
                listOf("https://example.com/1", "https://example.com/2", "https://example.com/3"),
                observed.map { it.url },
            )
        }

    @Test
    fun observeByCountryAndFeed_isScopedToCountryAndFeed() =
        runTest {
            val usArticles = listOf(sampleArticle(url = "https://example.com/us"))
            val plArticles = listOf(sampleArticle(url = "https://example.com/pl"))
            dao.replaceAll(COUNTRY_US, TOP_HEADLINES_FEED, usArticles.toEntities(COUNTRY_US, TOP_HEADLINES_FEED))
            dao.replaceAll(COUNTRY_PL, TOP_HEADLINES_FEED, plArticles.toEntities(COUNTRY_PL, TOP_HEADLINES_FEED))

            val observed = dao.observeByCountryAndFeed(COUNTRY_US, TOP_HEADLINES_FEED).first()

            assertEquals(listOf("https://example.com/us"), observed.map { it.url })
        }

    private companion object {
        const val COUNTRY_US = "us"
        const val COUNTRY_PL = "pl"

        fun sampleArticle(
            url: String,
            title: String = "Title for $url",
        ): Article =
            Article(
                title = title,
                description = "Description for $url",
                url = url,
                imageUrl = "https://example.com/image.jpg",
                source = Source(id = "source", name = "Example Source"),
            )

        fun List<Article>.toEntities(
            country: String,
            feed: String,
        ) = mapIndexed { index, article -> article.toEntity(country, feed, index) }
    }
}
