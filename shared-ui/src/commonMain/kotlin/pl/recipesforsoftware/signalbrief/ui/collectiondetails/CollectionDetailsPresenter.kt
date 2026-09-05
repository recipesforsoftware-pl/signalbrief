package pl.recipesforsoftware.signalbrief.ui.collectiondetails

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

/** Framework-independent state holder for Collection Details. */
class CollectionDetailsPresenter(
    collection: Collection,
    collectionsRepository: CollectionsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _uiState = MutableStateFlow(CollectionDetailsUiState(collection))
    val uiState: StateFlow<CollectionDetailsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            collectionsRepository.observeArticlesInCollection(collection.id).collect { articles ->
                _uiState.value = _uiState.value.copy(articles = articles)
            }
        }
    }

    fun dispose() = scope.cancel()
}
