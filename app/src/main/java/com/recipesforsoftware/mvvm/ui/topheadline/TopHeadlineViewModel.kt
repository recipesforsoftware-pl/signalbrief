package com.recipesforsoftware.mvvm.ui.topheadline

import androidx.lifecycle.ViewModel
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesPresenter
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesUiState
import com.recipesforsoftware.mvvm.utils.AppConstant.COUNTRY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TopHeadlineViewModel
    @Inject
    constructor(
        repository: NewsRepository,
    ) : ViewModel() {
        private val presenter =
            TopHeadlinesPresenter(
                repository = repository,
                country = COUNTRY,
                dispatcher = Dispatchers.Main.immediate,
            )

        val uiState: StateFlow<TopHeadlinesUiState> = presenter.uiState

        fun refresh() {
            presenter.refresh()
        }

        override fun onCleared() {
            presenter.dispose()
            super.onCleared()
        }
    }
