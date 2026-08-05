package pl.recipesforsoftware.signalbrief.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

@Singleton
class ThemePreference
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private object Keys {
            val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        }

        val isDarkModeEnabled: Flow<Boolean> =
            context.themeDataStore.data.map { preferences ->
                preferences[Keys.DARK_MODE_ENABLED] ?: false
            }

        suspend fun setDarkMode(enabled: Boolean) {
            context.themeDataStore.edit { preferences ->
                preferences[Keys.DARK_MODE_ENABLED] = enabled
            }
        }
    }
