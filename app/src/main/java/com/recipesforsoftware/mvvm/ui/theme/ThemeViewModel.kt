package com.recipesforsoftware.mvvm.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel
    @Inject
    constructor(
        private val themePreference: ThemePreference,
    ) : ViewModel() {
        val isDarkMode =
            themePreference.isDarkModeEnabled
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false,
                )

        fun toggleDarkMode() {
            viewModelScope.launch {
                val current = themePreference.isDarkModeEnabled.first()
                themePreference.setDarkMode(!current)
            }
        }

        fun setDarkMode(enabled: Boolean) {
            viewModelScope.launch {
                themePreference.setDarkMode(enabled)
            }
        }
    }
