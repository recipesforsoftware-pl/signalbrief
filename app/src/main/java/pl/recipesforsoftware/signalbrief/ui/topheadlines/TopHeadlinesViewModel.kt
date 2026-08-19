package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesConfig.DEFAULT_COUNTRY
import javax.inject.Inject

@HiltViewModel
class TopHeadlinesViewModel
    @Inject
    constructor(
        repository: NewsRepository,
        savedArticlesRepository: SavedArticlesRepository,
    ) : ViewModel() {
        private val presenter =
            TopHeadlinesPresenter(
                repository = repository,
                savedArticlesRepository = savedArticlesRepository,
                country = DEFAULT_COUNTRY,
                dispatcher = Dispatchers.Main.immediate,
            )

        val uiState: StateFlow<TopHeadlinesUiState> = presenter.uiState

        fun refresh() {
            presenter.refresh()
        }

        fun toggleBookmark(article: Article) {
            presenter.toggleBookmark(article)
        }

        override fun onCleared() {
            presenter.dispose()
        }
    }
