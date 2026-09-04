package pl.recipesforsoftware.signalbrief.ui.collections

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel
    @Inject
    constructor(
        repository: CollectionsRepository,
    ) : ViewModel() {
        private val presenter = CollectionsPresenter(repository, Dispatchers.Main.immediate)
        val uiState: StateFlow<CollectionsUiState> = presenter.uiState

        fun openCreateEditor() = presenter.openCreateEditor()

        fun openRenameEditor(collection: Collection) = presenter.openRenameEditor(collection)

        fun updateEditorName(name: String) = presenter.updateEditorName(name)

        fun confirmEditor() = presenter.confirmEditor()

        fun dismissEditor() = presenter.dismissEditor()

        fun openDeleteConfirmation(collection: Collection) = presenter.openDeleteConfirmation(collection)

        fun confirmDelete() = presenter.confirmDelete()

        fun dismissDeleteConfirmation() = presenter.dismissDeleteConfirmation()

        fun dismissError() = presenter.dismissError()

        override fun onCleared() = presenter.dispose()
    }
