package com.recipesforsoftware.mvvm.ui.topheadline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.recipesforsoftware.mvvm.ui.screens.TopHeadlineScreen
import com.recipesforsoftware.mvvm.ui.theme.NewsAppTheme

@AndroidEntryPoint
class TopHeadlineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NewsAppTheme {
                val viewModel: TopHeadlineViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                TopHeadlineScreen(
                    uiState = uiState,
                    onRefresh = viewModel::fetchTopHeadlines
                )
            }
        }
    }
}
