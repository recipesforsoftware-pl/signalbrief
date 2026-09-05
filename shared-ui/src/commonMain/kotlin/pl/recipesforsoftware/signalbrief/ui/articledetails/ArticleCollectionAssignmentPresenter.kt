package pl.recipesforsoftware.signalbrief.ui.articledetails

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
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

/**
 * Framework-independent state holder for assigning one article to collections.
 *
 * Both collection and membership state are repository-derived. Mutations do not
 * update selection optimistically: a successful repository emission is the only
 * thing that changes a row's checked state. A per-collection guard prevents
 * duplicate work while a toggle is in progress.
 */
class ArticleCollectionAssignmentPresenter(
    private val collectionsRepository: CollectionsRepository,
    private val article: Article,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow(ArticleCollectionAssignmentUiState())
    val uiState: StateFlow<ArticleCollectionAssignmentUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            collectionsRepository.observeAllCollections().collect { collections ->
                update { copy(collections = collections, isLoadingCollections = false) }
            }
        }
        scope.launch {
            collectionsRepository.observeCollectionIdsForArticle(article.url).collect { selectedIds ->
                update { copy(selectedCollectionIds = selectedIds) }
            }
        }
    }

    fun showPicker() = update { copy(isPickerVisible = true, error = null) }

    fun dismissPicker() = update { copy(isPickerVisible = false, error = null) }

    fun toggleCollection(collectionId: String) {
        val state = _uiState.value
        if (collectionId in state.mutatingCollectionIds) return

        val removeMembership = collectionId in state.selectedCollectionIds
        update { copy(mutatingCollectionIds = mutatingCollectionIds + collectionId, error = null) }
        scope.launch {
            val result =
                if (removeMembership) {
                    collectionsRepository.removeArticleFromCollection(article.url, collectionId)
                } else {
                    collectionsRepository.addArticleToCollection(article, collectionId)
                }
            result.fold(
                onSuccess = {
                    update { copy(mutatingCollectionIds = mutatingCollectionIds - collectionId) }
                },
                onFailure = { failure ->
                    update {
                        copy(
                            mutatingCollectionIds = mutatingCollectionIds - collectionId,
                            error = failure.toAssignmentError(),
                        )
                    }
                },
            )
        }
    }

    fun dispose() = scope.cancel()

    private fun update(transform: ArticleCollectionAssignmentUiState.() -> ArticleCollectionAssignmentUiState) {
        _uiState.value = _uiState.value.transform()
    }
}

private fun Throwable.toAssignmentError(): ArticleCollectionAssignmentError =
    when (this) {
        is CollectionFailure.NotFound -> ArticleCollectionAssignmentError.NotFound
        else -> ArticleCollectionAssignmentError.Unknown
    }
