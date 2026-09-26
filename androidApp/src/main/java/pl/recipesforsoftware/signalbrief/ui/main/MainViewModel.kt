package pl.recipesforsoftware.signalbrief.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

class MainViewModel(
    savedArticlesRepository: SavedArticlesRepository,
) : ViewModel() {
    val savedArticleCount: Flow<Int> = savedArticlesRepository.observeAllSavedArticles().map { it.size }
}
