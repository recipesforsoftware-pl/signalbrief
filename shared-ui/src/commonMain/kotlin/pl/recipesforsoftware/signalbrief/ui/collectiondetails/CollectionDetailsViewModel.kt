package pl.recipesforsoftware.signalbrief.ui.collectiondetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository

/** Framework-independent state holder for Collection Details. */
class CollectionDetailsViewModel(
    collection: Collection,
    collectionsRepository: CollectionsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionDetailsUiState(collection))
    val uiState: StateFlow<CollectionDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            collectionsRepository.observeArticlesInCollection(collection.id).collect { articles ->
                _uiState.value = _uiState.value.copy(articles = articles)
            }
        }
    }
}
