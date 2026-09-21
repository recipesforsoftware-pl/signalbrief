package pl.recipesforsoftware.signalbrief.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

/** Lifecycle-aware state holder for the Saved Articles screen. */
class SavedArticlesViewModel(
    private val savedArticlesRepository: SavedArticlesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SavedArticlesUiState>(SavedArticlesUiState.Loading)
    val uiState: StateFlow<SavedArticlesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            savedArticlesRepository.observeAllSavedArticles().collect { articles ->
                _uiState.value =
                    if (articles.isEmpty()) {
                        SavedArticlesUiState.Empty
                    } else {
                        SavedArticlesUiState.Content(articles)
                    }
            }
        }
    }

    /** Delegates persistence to the repository; its reactive stream updates the UI. */
    fun removeArticle(url: String) {
        viewModelScope.launch {
            savedArticlesRepository.removeSavedArticle(url)
        }
    }
}
