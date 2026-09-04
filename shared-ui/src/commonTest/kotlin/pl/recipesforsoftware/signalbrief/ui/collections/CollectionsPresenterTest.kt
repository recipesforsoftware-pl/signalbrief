package pl.recipesforsoftware.signalbrief.ui.collections

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private class FakeCollectionsRepository : CollectionsRepository {
    private val collections = MutableStateFlow<List<Collection>>(emptyList())
    var createFailure: Throwable? = null
    var renameFailure: Throwable? = null
    var deleteFailure: Throwable? = null
    var nextId = 1
    var createCalls = 0
    var deleteCalls = 0

    override fun observeAllCollections() = collections

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

@OptIn(ExperimentalCoroutinesApi::class)
private fun presenter(
    repository: CollectionsRepository,
    scope: TestScope,
) = CollectionsPresenter(repository, StandardTestDispatcher(scope.testScheduler))

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionsPresenterTest {
    @Test
    fun `initial empty repository observation is rendered`() =
        runTest {
            val presenter = presenter(FakeCollectionsRepository(), this)
            advanceUntilIdle()
            assertEquals(emptyList(), presenter.uiState.value.collections)
        }

    @Test
    fun `populated and subsequent repository observations are rendered`() =
        runTest {
            val repository = FakeCollectionsRepository()
            repository.emit(listOf(Collection("1", "Read later")))
            val presenter = presenter(repository, this)
            advanceUntilIdle()
            assertEquals(
                "Read later",
                presenter.uiState.value.collections
                    .single()
                    .name,
            )
            repository.emit(listOf(Collection("2", "Weekend")))
            advanceUntilIdle()
            assertEquals(
                "Weekend",
                presenter.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `create success closes editor and updates through repository observation`() =
        runTest {
            val repository = FakeCollectionsRepository()
            val presenter = presenter(repository, this)
            presenter.openCreateEditor()
            presenter.updateEditorName("  Read later  ")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.editor)
            assertEquals(
                "Read later",
                presenter.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `two immediate editor confirmations trigger one create`() =
        runTest {
            val repository = FakeCollectionsRepository()
            val presenter = presenter(repository, this)
            presenter.openCreateEditor()
            presenter.updateEditorName("Reading")

            presenter.confirmEditor()
            presenter.confirmEditor()
            advanceUntilIdle()

            assertEquals(1, repository.createCalls)
        }

    @Test
    fun `invalid and unknown create failures retain editor and expose mapped error`() =
        runTest {
            val repository = FakeCollectionsRepository()
            val presenter = presenter(repository, this)
            presenter.openCreateEditor()
            presenter.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.InvalidName, presenter.uiState.value.error)
            assertIs<CollectionsEditor.Create>(presenter.uiState.value.editor)
            repository.createFailure = IllegalStateException()
            presenter.updateEditorName("Reading")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.Unknown, presenter.uiState.value.error)
        }

    @Test
    fun `rename opens prefilled editor and success closes it`() =
        runTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val presenter = presenter(repository, this)
            advanceUntilIdle()
            presenter.openRenameEditor(Collection("1", "Old"))
            assertEquals("Old", (presenter.uiState.value.editor as CollectionsEditor.Rename).name)
            presenter.updateEditorName("New")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.editor)
            assertEquals(
                "New",
                presenter.uiState.value.collections
                    .single()
                    .name,
            )
        }

    @Test
    fun `invalid rename failure retains editor`() =
        runTest {
            val presenter = presenter(FakeCollectionsRepository(), this)
            presenter.openRenameEditor(Collection("1", "Old"))
            presenter.updateEditorName(" ")
            presenter.confirmEditor()
            advanceUntilIdle()
            assertEquals(CollectionsError.InvalidName, presenter.uiState.value.error)
            assertIs<CollectionsEditor.Rename>(presenter.uiState.value.editor)
        }

    @Test
    fun `delete can be cancelled and success closes confirmation`() =
        runTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val presenter = presenter(repository, this)
            advanceUntilIdle()
            presenter.openDeleteConfirmation(Collection("1", "Old"))
            presenter.dismissDeleteConfirmation()
            assertNull(presenter.uiState.value.collectionPendingDeletion)
            presenter.openDeleteConfirmation(Collection("1", "Old"))
            presenter.confirmDelete()
            advanceUntilIdle()
            assertNull(presenter.uiState.value.collectionPendingDeletion)
            assertEquals(emptyList(), presenter.uiState.value.collections)
        }

    @Test
    fun `delete failure retains confirmation and exposes error`() =
        runTest {
            val repository = FakeCollectionsRepository().also { it.deleteFailure = CollectionFailure.NotFound }
            val presenter = presenter(repository, this)
            presenter.openDeleteConfirmation(Collection("1", "Old"))
            presenter.confirmDelete()
            advanceUntilIdle()
            assertEquals(CollectionsError.NotFound, presenter.uiState.value.error)
            assertEquals(
                "1",
                presenter.uiState.value.collectionPendingDeletion
                    ?.id,
            )
        }

    @Test
    fun `two immediate delete confirmations trigger one delete`() =
        runTest {
            val repository = FakeCollectionsRepository().also { it.emit(listOf(Collection("1", "Old"))) }
            val presenter = presenter(repository, this)
            advanceUntilIdle()
            presenter.openDeleteConfirmation(Collection("1", "Old"))

            presenter.confirmDelete()
            presenter.confirmDelete()
            advanceUntilIdle()

            assertEquals(1, repository.deleteCalls)
        }
}
