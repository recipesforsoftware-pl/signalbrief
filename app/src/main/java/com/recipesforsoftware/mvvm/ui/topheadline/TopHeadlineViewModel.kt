package com.recipesforsoftware.mvvm.ui.topheadline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipesforsoftware.mvvm.data.model.Article
import com.recipesforsoftware.mvvm.data.repository.TopHeadlineRepository
import com.recipesforsoftware.mvvm.ui.base.UiState
import com.recipesforsoftware.mvvm.utils.AppConstant.COUNTRY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopHeadlineViewModel
    @Inject
    constructor(
        private val repository: TopHeadlineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<UiState<List<Article>>>(UiState.Loading)
        val uiState: StateFlow<UiState<List<Article>>> = _uiState.asStateFlow()

        init {
            fetchTopHeadlines()
        }

        fun fetchTopHeadlines() {
            viewModelScope.launch {
                _uiState.value = UiState.Loading
                repository
                    .getTopHeadlines(COUNTRY)
                    .onSuccess { articles ->
                        _uiState.value = UiState.Success(articles)
                    }.onFailure { throwable ->
                        _uiState.value =
                            UiState.Error(
                                throwable.message ?: "An unexpected error occurred",
                            )
                    }
            }
        }
    }
