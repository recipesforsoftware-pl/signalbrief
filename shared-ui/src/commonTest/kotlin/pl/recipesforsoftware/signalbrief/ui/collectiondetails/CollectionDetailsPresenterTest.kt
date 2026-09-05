package pl.recipesforsoftware.signalbrief.ui.collectiondetails

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private class DetailsCollectionsRepository : CollectionsRepository {
    private val articles = MutableStateFlow<List<Article>>(emptyList())

    override fun observeAllCollections(): Flow<List<Collection>> = flowOf(emptyList())

    override fun observeCollectionIdsForArticle(articleId: String): Flow<Set<String>> = flowOf(emptySet())

    override fun observeArticlesInCollection(collectionId: String): Flow<List<Article>> = articles

    override suspend fun createCollection(name: String): Result<Collection> = error("Not used")

    override suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection> = error("Not used")

    override suspend fun deleteCollection(id: String): Result<Unit> = error("Not used")

    override suspend fun addArticleToCollection(
        article: Article,
        collectionId: String,
    ): Result<Unit> = error("Not used")

    override suspend fun removeArticleFromCollection(
        articleId: String,
        collectionId: String,
    ): Result<Unit> = error("Not used")

    fun emit(values: List<Article>) {
        articles.value = values
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailsPresenterTest {
    @Test
    fun `initial empty and reactive article snapshots are rendered`() =
        runTest {
            val repository = DetailsCollectionsRepository()
            val collection = Collection("1", "Reading")
            val presenter = CollectionDetailsPresenter(collection, repository, StandardTestDispatcher(testScheduler))

            advanceUntilIdle()
            assertEquals(collection, presenter.uiState.value.collection)
            assertEquals(emptyList(), presenter.uiState.value.articles)

            val article = Article("A title", null, "https://example.com/a", null, null)
            repository.emit(listOf(article))
            advanceUntilIdle()
            assertEquals(listOf(article), presenter.uiState.value.articles)
            presenter.dispose()
        }
}
