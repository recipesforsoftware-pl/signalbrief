package pl.recipesforsoftware.signalbrief.ui.articledetails

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

private class FakeAssignmentCollectionsRepository : CollectionsRepository {
    private val collections = MutableStateFlow<List<Collection>>(emptyList())
    private val memberships = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val collectionArticles = MutableStateFlow<List<Article>>(emptyList())
    val addCalls = mutableListOf<Pair<Article, String>>()
    val removeCalls = mutableListOf<Pair<String, String>>()
    var addResult: suspend () -> Result<Unit> = { Result.success(Unit) }
    var removeResult: suspend () -> Result<Unit> = { Result.success(Unit) }

    override fun observeAllCollections(): Flow<List<Collection>> = collections

    override fun observeCollectionIdsForArticle(articleId: String): Flow<Set<String>> = membershipsFor(articleId)

    override fun observeArticlesInCollection(collectionId: String): Flow<List<Article>> = collectionArticles

    override suspend fun createCollection(name: String): Result<Collection> = error("Not used")

    override suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection> = error("Not used")

    override suspend fun deleteCollection(id: String): Result<Unit> = error("Not used")

    override suspend fun addArticleToCollection(
        article: Article,
        collectionId: String,
    ): Result<Unit> {
        addCalls += article to collectionId
        return addResult().onSuccess {
            emitMembership(article.url, memberships.value[article.url].orEmpty() + collectionId)
        }
    }

    override suspend fun removeArticleFromCollection(
        articleId: String,
        collectionId: String,
    ): Result<Unit> {
        removeCalls += articleId to collectionId
        return removeResult().onSuccess {
            emitMembership(articleId, memberships.value[articleId].orEmpty() - collectionId)
        }
    }

    fun emitCollections(vararg values: Collection) {
        collections.value = values.toList()
    }

    fun emitMembership(
        articleId: String,
        ids: Set<String>,
    ) {
        memberships.value = memberships.value.toMutableMap().apply { put(articleId, ids) }
    }

    private fun membershipsFor(articleId: String): Flow<Set<String>> = memberships.map { it[articleId].orEmpty() }
}

private val assignmentArticle =
    Article(
        title = "Article",
        description = null,
        url = "https://example.com/article",
        imageUrl = null,
        source = Source("source", "Source"),
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun assignmentViewModel(repository: CollectionsRepository): ArticleCollectionAssignmentViewModel =
    ArticleCollectionAssignmentViewModel(
        repository,
        assignmentArticle,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleCollectionAssignmentViewModelTest {
    @Test
    fun `repository collections and membership form initial picker state`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("read", "Read later"))
            repository.emitMembership(assignmentArticle.url, setOf("read"))
            val viewModel = assignmentViewModel(repository)

            advanceUntilIdle()

            assertEquals(listOf(Collection("read", "Read later")), viewModel.uiState.value.collections)
            assertEquals(setOf("read"), viewModel.uiState.value.selectedCollectionIds)
            assertFalse(viewModel.uiState.value.isLoadingCollections)
        }

    @Test
    fun `adding calls repository and waits for membership emission to select`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("read", "Read later"))
            val emission = CompletableDeferred<Result<Unit>>()
            repository.addResult = { emission.await() }
            val viewModel = assignmentViewModel(repository)
            advanceUntilIdle()

            viewModel.toggleCollection("read")
            advanceUntilIdle()
            assertEquals(listOf(assignmentArticle to "read"), repository.addCalls)
            assertFalse("read" in viewModel.uiState.value.selectedCollectionIds)

            emission.complete(Result.success(Unit))
            advanceUntilIdle()
            assertTrue("read" in viewModel.uiState.value.selectedCollectionIds)
        }

    @Test
    fun `removing calls repository and waits for membership emission to unselect`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("read", "Read later"))
            repository.emitMembership(assignmentArticle.url, setOf("read"))
            val viewModel = assignmentViewModel(repository)
            advanceUntilIdle()

            viewModel.toggleCollection("read")
            advanceUntilIdle()

            assertEquals(listOf(assignmentArticle.url to "read"), repository.removeCalls)
            assertFalse("read" in viewModel.uiState.value.selectedCollectionIds)
        }

    @Test
    fun `duplicate toggle while mutation is in flight is ignored`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("read", "Read later"))
            val emission = CompletableDeferred<Result<Unit>>()
            repository.addResult = { emission.await() }
            val viewModel = assignmentViewModel(repository)
            advanceUntilIdle()

            viewModel.toggleCollection("read")
            viewModel.toggleCollection("read")
            advanceUntilIdle()

            assertEquals(1, repository.addCalls.size)
            emission.complete(Result.success(Unit))
            advanceUntilIdle()
        }

    @Test
    fun `failed mutation preserves persisted selection and exposes friendly error`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("read", "Read later"))
            repository.addResult = { Result.failure(IllegalStateException("storage failed")) }
            val viewModel = assignmentViewModel(repository)
            advanceUntilIdle()

            viewModel.toggleCollection("read")
            advanceUntilIdle()

            assertFalse("read" in viewModel.uiState.value.selectedCollectionIds)
            assertEquals(ArticleCollectionAssignmentError.Unknown, viewModel.uiState.value.error)
        }

    @Test
    fun `empty repository state remains renderable and can open picker`() =
        runViewModelTest {
            val viewModel = assignmentViewModel(FakeAssignmentCollectionsRepository())
            advanceUntilIdle()

            viewModel.showPicker()

            assertTrue(viewModel.uiState.value.isPickerVisible)
            assertTrue(
                viewModel.uiState.value.collections
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isLoadingCollections)
        }

    @Test
    fun `external membership changes update an open picker`() =
        runViewModelTest {
            val repository = FakeAssignmentCollectionsRepository()
            repository.emitCollections(Collection("weekend", "Weekend"))
            val viewModel = assignmentViewModel(repository)
            advanceUntilIdle()
            viewModel.showPicker()

            repository.emitMembership(assignmentArticle.url, setOf("weekend"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isPickerVisible)
            assertEquals(setOf("weekend"), viewModel.uiState.value.selectedCollectionIds)
        }
}
