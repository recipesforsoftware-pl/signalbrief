package pl.recipesforsoftware.signalbrief.ui.collections

import androidx.lifecycle.ViewModelStore
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
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class FakeCollectionsRepository : CollectionsRepository {
    private val collections = MutableStateFlow<List<Collection>>(emptyList())
    private val memberships = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    private val collectionArticles = MutableStateFlow<List<Article>>(emptyList())
    var createFailure: Throwable? = null
    var renameFailure: Throwable? = null
    var deleteFailure: Throwable? = null
    var nextId = 1
    var createCalls = 0
    var deleteCalls = 0

    override fun observeAllCollections() = collections

    override fun observeCollectionIdsForArticle(articleId: String): Flow<Set<String>> =
        memberships.map { perArticle -> perArticle[articleId].orEmpty() }

    override fun observeArticlesInCollection(collectionId: String): Flow<List<Article>> = collectionArticles

    override suspend fun addArticleToCollection(
        article: Article,
        collectionId: String,
    ): Result<Unit> {
        val perArticle = memberships.value.toMutableMap()
        perArticle[article.url] = perArticle[article.url].orEmpty() + collectionId
        memberships.value = perArticle
        return Result.success(Unit)
    }

    override suspend fun removeArticleFromCollection(
        articleId: String,
        collectionId: String,
    ): Result<Unit> {
        val perArticle = memberships.value.toMutableMap()
        perArticle[articleId] = perArticle[articleId].orEmpty() - collectionId
        memberships.value = perArticle
        return Result.success(Unit)
    }

    @Suppress("ReturnCount")
    override suspend fun createCollection(name: String): Result<Collection> {
        createCalls++
        createFailure?.let { return Result.failure(it) }
        if (name.isBlank()) return Result.failure(CollectionFailure.InvalidName)
        val collection = Collection(nextId++.toString(), name.trim())
        collections.value = listOf(collection) + collections.value
        return Result.success(collection)
    }

    @Suppress("ReturnCount")
    override suspend fun renameCollection(
        id: String,
        newName: String,
    ): Result<Collection> {
        renameFailure?.let { return Result.failure(it) }
        if (newName.isBlank()) return Result.failure(CollectionFailure.InvalidName)
        val existing = collections.value.find { it.id == id } ?: return Result.failure(CollectionFailure.NotFound)
        val updated = existing.copy(name = newName.trim())
        collections.value = collections.value.map { if (it.id == id) updated else it }
        return Result.success(updated)
    }

    override suspend fun deleteCollection(id: String): Result<Unit> =
        deleteFailure?.let {
            deleteCalls++
            Result.failure(it)
        }
            ?: if (collections.value.none { it.id == id }) {
                deleteCalls++
                Result.failure(CollectionFailure.NotFound)
            } else {
                deleteCalls++
                collections.value = collections.value.filterNot { it.id == id }
                Result.success(Unit)
            }

    fun emit(values: List<Collection>) {
        collections.value = values
    }
}

private fun createViewModel(repository: CollectionsRepository) = CollectionsViewModel(repository)

@OptIn(ExperimentalCoroutinesApi::class)
private fun runCollectionsViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsViewModelTest {
    @Test
    fun `initial empty repository observation is rendered`() =
        runCollectionsViewModelTest {
            val viewModel = createViewModel(FakeCollectionsRepository())
            advanceUntilIdle()
            assertEquals(emptyList(), viewModel.uiState.value.collections)
        }

    @Test
    fun `populated and subsequent repository observations are rendered`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository()
            repository.emit(listOf(Collection("1", "Read later")))
            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            assertEquals(
                "Read later",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
            repository.emit(listOf(Collection("2", "Weekend")))
            advanceUntilIdle()
            assertEquals(
                "Weekend",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `create success closes editor and updates through repository observation`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository()
            val viewModel = createViewModel(repository)
            viewModel.openCreateEditor()
            viewModel.updateEditorName("  Read later  ")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.editor)
            assertEquals(
                "Read later",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `two immediate editor confirmations trigger one create`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository()
            val viewModel = createViewModel(repository)
            viewModel.openCreateEditor()
            viewModel.updateEditorName("Reading")
            viewModel.confirmEditor()
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(1, repository.createCalls)
        }

    @Test
    fun `invalid and unknown create failures retain editor and expose mapped error`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository()
            val viewModel = createViewModel(repository)
            viewModel.openCreateEditor()
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.InvalidName, viewModel.uiState.value.error)
            assertIs<CollectionsEditor.Create>(viewModel.uiState.value.editor)
            repository.createFailure = IllegalStateException()
            viewModel.updateEditorName("Reading")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.Unknown, viewModel.uiState.value.error)
        }

    @Test
    fun `rename opens prefilled editor and success closes it`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            viewModel.openRenameEditor(Collection("1", "Old"))
            assertEquals("Old", (viewModel.uiState.value.editor as CollectionsEditor.Rename).name)
            viewModel.updateEditorName("New")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.editor)
            assertEquals(
                "New",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `invalid rename failure retains editor`() =
        runCollectionsViewModelTest {
            val viewModel = createViewModel(FakeCollectionsRepository())
            viewModel.openRenameEditor(Collection("1", "Old"))
            viewModel.updateEditorName(" ")
            viewModel.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.InvalidName, viewModel.uiState.value.error)
            assertIs<CollectionsEditor.Rename>(viewModel.uiState.value.editor)
        }

    @Test
    fun `delete can be cancelled and success closes confirmation`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            viewModel.openDeleteConfirmation(Collection("1", "Old"))
            viewModel.dismissDeleteConfirmation()
            assertNull(viewModel.uiState.value.collectionPendingDeletion)
            viewModel.openDeleteConfirmation(Collection("1", "Old"))
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.collectionPendingDeletion)
            assertEquals(emptyList(), viewModel.uiState.value.collections)
        }

    @Test
    fun `delete failure retains confirmation and exposes error`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository().also { it.deleteFailure = CollectionFailure.NotFound }
            val viewModel = createViewModel(repository)
            viewModel.openDeleteConfirmation(Collection("1", "Old"))
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertEquals(CollectionsError.NotFound, viewModel.uiState.value.error)
            assertEquals(
                "1",
                viewModel.uiState.value.collectionPendingDeletion
                    ?.id,
            )
        }

    @Test
    fun `two immediate delete confirmations trigger one delete`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val viewModel = createViewModel(repository)
            advanceUntilIdle()
            viewModel.openDeleteConfirmation(Collection("1", "Old"))
            viewModel.confirmDelete()
            viewModel.confirmDelete()
            advanceUntilIdle()
            assertEquals(1, repository.deleteCalls)
        }

    @Test
    fun `clearing the lifecycle owner stops later observation updates`() =
        runCollectionsViewModelTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Read later"))) }
            val viewModel = createViewModel(repository)
            val viewModelStore = ViewModelStore()
            viewModelStore.put("collections", viewModel)
            advanceUntilIdle()
            assertEquals(
                "Read later",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
            viewModelStore.clear()
            advanceUntilIdle()
            repository.emit(listOf(Collection("2", "Weekend")))
            advanceUntilIdle()
            assertEquals(
                "Read later",
                viewModel.uiState.value.collections
                    .single()
                    .name,
            )
        }
}
