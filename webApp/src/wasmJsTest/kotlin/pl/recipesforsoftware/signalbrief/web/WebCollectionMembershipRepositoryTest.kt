package pl.recipesforsoftware.signalbrief.web

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebCollectionMembershipRepositoryTest {
    @Test
    fun membershipIsReactiveIdempotentAndPersistsAcrossReload() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()
            val watchlist = repository.createCollection("Watchlist").getOrThrow()

            assertEquals(emptySet(), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)
            assertEquals(setOf(reading.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)
            assertEquals(setOf(reading.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), watchlist.id).isSuccess)
            assertEquals(setOf(reading.id, watchlist.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(repository.removeArticleFromCollection(ARTICLE_A, reading.id).isSuccess)
            assertEquals(setOf(watchlist.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())

            assertTrue(repository.addArticleToCollection(article(ARTICLE_B), reading.id).isSuccess)
            assertEquals(setOf(watchlist.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertEquals(setOf(reading.id), repository.observeCollectionIdsForArticle(ARTICLE_B).first())
            assertEquals(
                setOf(watchlist.id),
                WebCollectionsRepository(storage).observeCollectionIdsForArticle(ARTICLE_A).first(),
            )
        }

    @Test
    fun membershipStoresDurableArticleSnapshot() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()

            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)

            val membership = storage.memberships().single()
            assertEquals(reading.id.toLong(), membership.collectionId)
            assertEquals(ARTICLE_A, membership.articleId)
            assertEquals("Headline for $ARTICLE_A", membership.title)
            assertEquals("Summary for $ARTICLE_A", membership.description)
            assertEquals("https://example.com/image.png", membership.imageUrl)
            assertEquals("source-id", membership.source?.id)
            assertEquals("Source Name", membership.source?.name)

            val reloaded = WebCollectionsRepository(storage)
            assertEquals(setOf(reading.id), reloaded.observeCollectionIdsForArticle(ARTICLE_A).first())
        }

    @Test
    fun activeObserverSeesMembershipChangesInOrder() =
        runTest {
            val repository = WebCollectionsRepository(FakeCollectionsStorage())
            val reading = repository.createCollection("Reading").getOrThrow()
            val watchlist = repository.createCollection("Watchlist").getOrThrow()

            repository.observeCollectionIdsForArticle(ARTICLE_A).test {
                assertEquals(emptySet(), awaitItem())
                assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)
                assertEquals(setOf(reading.id), awaitItem())
                assertTrue(repository.addArticleToCollection(article(ARTICLE_A), watchlist.id).isSuccess)
                assertEquals(setOf(reading.id, watchlist.id), awaitItem())
                assertTrue(repository.removeArticleFromCollection(ARTICLE_A, reading.id).isSuccess)
                assertEquals(setOf(watchlist.id), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun membershipValidatesInputAndCollectionDeletionCleansItUp() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()

            assertIs<CollectionFailure.InvalidArticleId>(
                repository.addArticleToCollection(article(" "), reading.id).exceptionOrNull(),
            )
            assertIs<CollectionFailure.NotFound>(
                repository.addArticleToCollection(article(ARTICLE_A), "missing").exceptionOrNull(),
            )
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)
            assertTrue(repository.deleteCollection(reading.id).isSuccess)
            assertEquals(emptySet(), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
        }

    @Test
    fun malformedOrUnreadableMembershipStorageRestoresSafely() =
        runTest {
            val malformed =
                WebCollectionsRepository(
                    FakeCollectionsStorage(
                        values = mutableMapOf(COLLECTION_MEMBERSHIPS_STORAGE_KEY to "not json"),
                    ),
                )
            assertEquals(emptySet(), malformed.observeCollectionIdsForArticle(ARTICLE_A).first())

            val unreadable =
                WebCollectionsRepository(FakeCollectionsStorage(readFailure = IllegalStateException("read")))
            assertEquals(emptySet(), unreadable.observeCollectionIdsForArticle(ARTICLE_A).first())
        }

    @Test
    fun failedMembershipWriteLeavesObservableStateUnchanged() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()

            storage.writeFailure = IllegalStateException("write")
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isFailure)
            assertEquals(emptySet(), repository.observeCollectionIdsForArticle(ARTICLE_A).first())

            storage.writeFailure = null
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)

            storage.writeFailure = IllegalStateException("write")
            assertTrue(repository.removeArticleFromCollection(ARTICLE_A, reading.id).isFailure)
            assertEquals(setOf(reading.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
        }

    @Test
    fun failedCollectionWriteLeavesBothObservableStatesUnchanged() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)

            storage.failKeys.add(COLLECTIONS_STORAGE_KEY)
            assertTrue(repository.deleteCollection(reading.id).isFailure)

            assertEquals(listOf(reading), repository.observeAllCollections().first())
            assertEquals(setOf(reading.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertEquals(1, storage.memberships().size)
        }

    @Test
    fun failedMembershipCleanupStillYieldsLogicallyDeletedCollection() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)

            storage.failKeys.add(COLLECTION_MEMBERSHIPS_STORAGE_KEY)
            assertTrue(repository.deleteCollection(reading.id).isSuccess)

            assertEquals(emptyList(), repository.observeAllCollections().first())
            assertEquals(emptySet(), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(storage.memberships().isNotEmpty())
        }

    @Test
    fun reloadAfterPartialCleanupExposesNoOrphanMembership() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)

            storage.failKeys.add(COLLECTION_MEMBERSHIPS_STORAGE_KEY)
            assertTrue(repository.deleteCollection(reading.id).isSuccess)
            assertTrue(storage.memberships().isNotEmpty())

            storage.failKeys.clear()
            val reloaded = WebCollectionsRepository(storage)
            assertEquals(emptySet(), reloaded.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(storage.memberships().isEmpty())
        }

    @Test
    fun laterSuccessfulMembershipWriteCleansStaleOrphanStorage() =
        runTest {
            val storage = FakeCollectionsStorage()
            val repository = WebCollectionsRepository(storage)
            val reading = repository.createCollection("Reading").getOrThrow()
            val watchlist = repository.createCollection("Watchlist").getOrThrow()
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), reading.id).isSuccess)
            assertTrue(repository.addArticleToCollection(article(ARTICLE_A), watchlist.id).isSuccess)

            storage.failKeys.add(COLLECTION_MEMBERSHIPS_STORAGE_KEY)
            assertTrue(repository.deleteCollection(reading.id).isSuccess)
            assertEquals(setOf(watchlist.id), repository.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertTrue(storage.memberships().any { it.collectionId == reading.id.toLong() })

            storage.failKeys.clear()
            assertTrue(repository.addArticleToCollection(article(ARTICLE_B), watchlist.id).isSuccess)

            val persisted = storage.memberships()
            assertTrue(persisted.none { it.collectionId == reading.id.toLong() })
            assertEquals(setOf(ARTICLE_A, ARTICLE_B), persisted.map { it.articleId }.toSet())

            val reloaded = WebCollectionsRepository(storage)
            assertEquals(setOf(watchlist.id), reloaded.observeCollectionIdsForArticle(ARTICLE_A).first())
            assertEquals(setOf(watchlist.id), reloaded.observeCollectionIdsForArticle(ARTICLE_B).first())
        }

    private companion object {
        const val ARTICLE_A = "https://example.com/a"
        const val ARTICLE_B = "https://example.com/b"
    }
}

private fun FakeCollectionsStorage.memberships(): List<PersistedMembership> {
    val payload = values[COLLECTION_MEMBERSHIPS_STORAGE_KEY] ?: return emptyList()
    return Json.decodeFromString(payload)
}

private fun article(url: String): Article =
    Article(
        title = "Headline for $url",
        description = "Summary for $url",
        url = url,
        imageUrl = "https://example.com/image.png",
        source = Source(id = "source-id", name = "Source Name"),
    )
