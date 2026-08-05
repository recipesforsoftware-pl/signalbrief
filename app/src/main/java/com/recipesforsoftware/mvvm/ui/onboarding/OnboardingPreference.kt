package com.recipesforsoftware.mvvm.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "onboarding_preferences",
)

/**
 * Hilt module providing the production onboarding DataStore.
 *
 * The DataStore is separated from [OnboardingPreference] so tests can inject an
 * isolated in-memory or temporary-file DataStore instead of sharing the
 * application-scoped singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingModule {
    @Provides
    @Singleton
    fun provideOnboardingDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.onboardingDataStore
}

/**
 * Android DataStore-backed persistence for onboarding completion.
 *
 * Keeps the DataStore as a constructor dependency so tests can provide an
 * isolated store. The production store is provided by [OnboardingModule].
 */
@Singleton
class OnboardingPreference
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        }

        /** `false` until the user completes or skips onboarding. */
        val isOnboardingCompleted: Flow<Boolean> =
            dataStore.data.map { preferences ->
                preferences[Keys.ONBOARDING_COMPLETED] ?: false
            }

        suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.ONBOARDING_COMPLETED] = completed
            }
        }
    }
