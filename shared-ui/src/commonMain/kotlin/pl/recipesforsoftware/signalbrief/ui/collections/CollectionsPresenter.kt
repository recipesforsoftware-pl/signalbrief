package pl.recipesforsoftware.signalbrief.ui.collections

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.failure.CollectionFailure
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

/** Framework-independent state holder for the Collections management screen. */
class CollectionsPresenter(
    private val collectionsRepository: CollectionsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            collectionsRepository.observeAllCollections().collect { collections ->
                _uiState.value = _uiState.value.copy(collections = collections)
            }
        }
    }

    fun openCreateEditor() = update { copy(editor = CollectionsEditor.Create(), error = null) }

    fun openRenameEditor(collection: Collection) =
        update {
            copy(
                editor = CollectionsEditor.Rename(collection),
                error = null,
            )
        }

    fun updateEditorName(name: String) =
        update {
            copy(
                editor =
                    when (val current = editor) {
                        is CollectionsEditor.Create -> current.copy(name = name)
                        is CollectionsEditor.Rename -> current.copy(name = name)
                        null -> null
                    },
            )
        }

    fun dismissEditor() = update { copy(editor = null, error = null) }

    fun confirmEditor() {
        val state = _uiState.value
        val editor = state.editor ?: return
        if (state.isSubmitting) return
        update { copy(isSubmitting = true, error = null) }
        scope.launch {
            val result =
                when (editor) {
                    is CollectionsEditor.Create -> {
                        collectionsRepository.createCollection(editor.name)
                    }

                    is CollectionsEditor.Rename -> {
                        collectionsRepository.renameCollection(editor.collection.id, editor.name)
                    }
                }
            result.fold(
                onSuccess = { update { copy(editor = null, isSubmitting = false) } },
                onFailure = { failure -> update { copy(isSubmitting = false, error = failure.toUiError()) } },
            )
        }
    }

    fun openDeleteConfirmation(collection: Collection) =
        update {
            copy(
                collectionPendingDeletion = collection,
                error = null,
            )
        }

    fun dismissDeleteConfirmation() = update { copy(collectionPendingDeletion = null) }

    fun confirmDelete() {
        val state = _uiState.value
        val collection = state.collectionPendingDeletion ?: return
        if (state.isSubmitting) return
        update { copy(isSubmitting = true, error = null) }
        scope.launch {
            collectionsRepository.deleteCollection(collection.id).fold(
                onSuccess = { update { copy(collectionPendingDeletion = null, isSubmitting = false) } },
                onFailure = { failure -> update { copy(isSubmitting = false, error = failure.toUiError()) } },
            )
        }
    }

    fun dismissError() = update { copy(error = null) }

    fun dispose() = scope.cancel()

    private fun update(transform: CollectionsUiState.() -> CollectionsUiState) {
        _uiState.value = _uiState.value.transform()
    }
}

private fun Throwable.toUiError(): CollectionsError =
    when (this) {
        is CollectionFailure.InvalidName -> CollectionsError.InvalidName
        is CollectionFailure.NotFound -> CollectionsError.NotFound
        else -> CollectionsError.Unknown
    }
