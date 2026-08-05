package pl.recipesforsoftware.signalbrief.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
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

        /**
         * `false` until the user completes or skips onboarding.
         *
         * A recoverable `IOException` while reading the DataStore (for example a
         * corrupt or unreadable file) falls back to an empty preference set, so
         * onboarding is treated as not completed instead of leaving the shell
         * stuck on the loading gate. Non-IO exceptions and coroutine
         * cancellation are rethrown unchanged.
         */
        val isOnboardingCompleted: Flow<Boolean> =
            dataStore.data
                .catch { throwable ->
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    if (throwable is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw throwable
                    }
                }.map { preferences ->
                    preferences[Keys.ONBOARDING_COMPLETED] ?: false
                }

        suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { preferences ->
                preferences[Keys.ONBOARDING_COMPLETED] = completed
            }
        }
    }
