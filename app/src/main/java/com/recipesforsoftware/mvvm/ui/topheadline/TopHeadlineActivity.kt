package com.recipesforsoftware.mvvm.ui.topheadline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.recipesforsoftware.mvvm.ui.theme.NewsAppTheme
import com.recipesforsoftware.mvvm.ui.theme.ThemeViewModel
import com.recipesforsoftware.mvvm.ui.topheadlines.TopHeadlinesScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TopHeadlineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            NewsAppTheme(isDarkMode = isDarkMode) {
                val viewModel: TopHeadlineViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                TopHeadlinesScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refresh,
                    topBarActions = {
                        DarkModeMenu(
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = themeViewModel::toggleDarkMode,
                        )
                    },
                )
            }
        }
    }
}
