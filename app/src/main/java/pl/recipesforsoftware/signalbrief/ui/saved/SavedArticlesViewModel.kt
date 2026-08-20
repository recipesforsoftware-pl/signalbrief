package pl.recipesforsoftware.signalbrief.ui.saved

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import javax.inject.Inject

@HiltViewModel
class SavedArticlesViewModel
    @Inject
    constructor(
        savedArticlesRepository: SavedArticlesRepository,
    ) : ViewModel() {
        private val presenter =
            SavedArticlesPresenter(
                savedArticlesRepository = savedArticlesRepository,
                dispatcher = Dispatchers.Main.immediate,
            )

        val uiState: StateFlow<SavedArticlesUiState> = presenter.uiState

        fun removeArticle(url: String) {
            presenter.removeArticle(url)
        }

        override fun onCleared() {
            presenter.dispose()
        }
    }
