package pl.recipesforsoftware.signalbrief.ui.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.ui.lifecycle.ScreenViewModelScope

@Composable
internal fun CollectionsRoute(
    collectionsRepository: CollectionsRepository,
    onBack: () -> Unit,
    onCollectionClick: (Collection) -> Unit,
) {
    ScreenViewModelScope {
        val viewModel: CollectionsViewModel = viewModel { CollectionsViewModel(collectionsRepository) }
        val uiState by viewModel.uiState.collectAsState()
        CollectionsScreen(
            uiState = uiState,
            onOpenCreateEditor = viewModel::openCreateEditor,
            onOpenRenameEditor = viewModel::openRenameEditor,
            onUpdateEditorName = viewModel::updateEditorName,
            onConfirmEditor = viewModel::confirmEditor,
            onDismissEditor = viewModel::dismissEditor,
            onOpenDeleteConfirmation = viewModel::openDeleteConfirmation,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
            onDismissError = viewModel::dismissError,
            onOpenCollection = onCollectionClick,
            onBack = onBack,
        )
    }
}
