package pl.recipesforsoftware.signalbrief.data.local.db

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.data.local.dao.ArticleCollectionMembershipDao
import pl.recipesforsoftware.signalbrief.data.local.entity.ArticleCollectionMembershipEntity
import pl.recipesforsoftware.signalbrief.data.local.entity.CollectionEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleCollectionMembershipDaoTest {
    private lateinit var database: SignalBriefDatabase
    private lateinit var membershipDao: ArticleCollectionMembershipDao

    @BeforeTest
    fun setUp() {
        database = createTestDatabase()
        membershipDao = database.articleCollectionMembershipDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserve_isUniqueAndIsolatedByArticle() =
        runTest {
            val firstCollection = collection("Reading")
            val secondCollection = collection("Watchlist")
            membershipDao.insertIgnore(membership(firstCollection, ARTICLE_A))
            membershipDao.insertIgnore(membership(secondCollection, ARTICLE_A))
            membershipDao.insertIgnore(membership(firstCollection, ARTICLE_A))
            membershipDao.insertIgnore(membership(firstCollection, ARTICLE_B))

            membershipDao.observeCollectionIdsForArticle(ARTICLE_A).test {
                assertEquals(listOf(firstCollection, secondCollection), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            membershipDao.observeCollectionIdsForArticle(ARTICLE_B).test {
                assertEquals(listOf(firstCollection), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deleteAndCollectionCascade_removeOnlyTheRequestedRelation() =
        runTest {
            val firstCollection = collection("Reading")
            val secondCollection = collection("Watchlist")
            membershipDao.insertIgnore(membership(firstCollection, ARTICLE_A))
            membershipDao.insertIgnore(membership(secondCollection, ARTICLE_A))

            assertEquals(1, membershipDao.delete(firstCollection, ARTICLE_A))
            assertEquals(0, membershipDao.delete(firstCollection, ARTICLE_A))
            membershipDao.observeCollectionIdsForArticle(ARTICLE_A).test {
                assertEquals(listOf(secondCollection), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            database.collectionDao().deleteById(secondCollection)
            membershipDao.observeCollectionIdsForArticle(ARTICLE_A).test {
                assertTrue(awaitItem().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun membership(
        collectionId: Long,
        articleUrl: String,
    ): ArticleCollectionMembershipEntity =
        ArticleCollectionMembershipEntity(
            collectionId = collectionId,
            articleId = articleUrl,
            title = "Stored headline",
            description = "Stored summary",
            imageUrl = "https://example.com/image.png",
            sourceId = "source-id",
            sourceName = "Example Source",
        )

    private suspend fun collection(name: String): Long =
        database.collectionDao().insert(
            CollectionEntity(name = name, createdAt = 1000L),
        )

    private companion object {
        const val ARTICLE_A = "https://example.com/a"
        const val ARTICLE_B = "https://example.com/b"
    }
}
