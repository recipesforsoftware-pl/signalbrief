package pl.recipesforsoftware.signalbrief

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Instrumented test for ThemePreference DataStore behavior.
 *
 * Tests the actual DataStore read/write cycle using a dedicated test DataStore
 * to avoid interfering with production preferences.
 */
class ThemePreferenceInstrumentedTest {
    private val Context.testDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "test_theme_preferences",
    )

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear DataStore before each test to ensure isolation
        runTest {
            context.testDataStore.edit { it.clear() }
        }
    }

    @Test
    fun dataStore_defaultsToFalse() =
        runTest {
            // When
            val preferences = context.testDataStore.data.first()

            // Then
            val key = booleanPreferencesKey("dark_mode_enabled")
            assertFalse(preferences[key] ?: false)
        }

    @Test
    fun dataStore_setAndGetTrue() =
        runTest {
            // Given
            val key = booleanPreferencesKey("dark_mode_enabled")

            // When
            context.testDataStore.edit { it[key] = true }
            val preferences = context.testDataStore.data.first()

            // Then
            assertTrue(preferences[key] ?: false)
        }

    @Test
    fun dataStore_setAndGetFalse() =
        runTest {
            // Given
            val key = booleanPreferencesKey("dark_mode_enabled")

            // When - set to true first, then back to false
            context.testDataStore.edit { it[key] = true }
            context.testDataStore.edit { it[key] = false }
            val preferences = context.testDataStore.data.first()

            // Then
            assertFalse(preferences[key] ?: true)
        }

    @Test
    fun dataStore_overwriteValue() =
        runTest {
            // Given
            val key = booleanPreferencesKey("dark_mode_enabled")

            // When
            context.testDataStore.edit { it[key] = true }
            context.testDataStore.edit { it[key] = true }
            context.testDataStore.edit { it[key] = false }
            val preferences = context.testDataStore.data.first()

            // Then - last write wins
            assertFalse(preferences[key] ?: true)
        }
}
