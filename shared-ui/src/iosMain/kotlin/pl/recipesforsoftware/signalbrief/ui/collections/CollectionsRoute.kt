package pl.recipesforsoftware.signalbrief.ui.collections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import pl.recipesforsoftware.signalbrief.domain.model.Collection

@Composable
internal fun CollectionsRoute(
    presenter: CollectionsPresenter,
    onBack: () -> Unit,
    onCollectionClick: (Collection) -> Unit,
) {
    val uiState by presenter.uiState.collectAsState()
    CollectionsScreen(
        uiState = uiState,
        onOpenCreateEditor = presenter::openCreateEditor,
        onOpenRenameEditor = presenter::openRenameEditor,
        onUpdateEditorName = presenter::updateEditorName,
        onConfirmEditor = presenter::confirmEditor,
        onDismissEditor = presenter::dismissEditor,
        onOpenDeleteConfirmation = presenter::openDeleteConfirmation,
        onConfirmDelete = presenter::confirmDelete,
        onDismissDeleteConfirmation = presenter::dismissDeleteConfirmation,
        onDismissError = presenter::dismissError,
        onOpenCollection = onCollectionClick,
        onBack = onBack,
    )
}
