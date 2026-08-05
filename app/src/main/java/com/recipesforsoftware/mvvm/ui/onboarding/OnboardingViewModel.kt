package com.recipesforsoftware.mvvm.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android ViewModel exposing onboarding completion state and the action to
 * complete onboarding.
 *
 * The initial state is `null` so the host can show a loading placeholder while
 * DataStore reads the persisted value, preventing an onboarding flash for
 * returning users.
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val onboardingPreference: OnboardingPreference,
    ) : ViewModel() {
        val isOnboardingCompleted: StateFlow<Boolean?> =
            onboardingPreference
                .isOnboardingCompleted
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        fun completeOnboarding() {
            viewModelScope.launch {
                onboardingPreference.setOnboardingCompleted(true)
            }
        }
    }
