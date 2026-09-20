package pl.recipesforsoftware.signalbrief.ui.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OnboardingPreferenceTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preference: OnboardingPreference

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = RuntimeEnvironment.getApplication()
        // Use a unique file per test so DataStore caching never leaks state.
        val dataStoreFile = context.filesDir.resolve("datastore/test_onboarding_${UUID.randomUUID()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create { dataStoreFile }
        preference = OnboardingPreference(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default is not completed`() =
        runTest(testDispatcher) {
            advanceUntilIdle()

            val value = preference.isOnboardingCompleted.first()
            assertFalse(value)
        }

    @Test
    fun `completing onboarding persists true`() =
        runTest(testDispatcher) {
            preference.setOnboardingCompleted(true)
            advanceUntilIdle()

            preference.isOnboardingCompleted.test {
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a new reader instance observes true`() =
        runTest(testDispatcher) {
            preference.setOnboardingCompleted(true)
            advanceUntilIdle()

            val newPreference = OnboardingPreference(dataStore)

            newPreference.isOnboardingCompleted.test {
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `IOException during read falls back to false`() =
        runTest(testDispatcher) {
            val failingDataStore = mockk<DataStore<Preferences>>()
            every { failingDataStore.data } returns flow { throw IOException("corrupt") }

            val preference = OnboardingPreference(failingDataStore)

            assertFalse(preference.isOnboardingCompleted.first())
        }

    @Test
    fun `non-IO exception during read is not hidden`() =
        runTest(testDispatcher) {
            val failingDataStore = mockk<DataStore<Preferences>>()
            every { failingDataStore.data } returns flow { throw IllegalStateException("boom") }

            val preference = OnboardingPreference(failingDataStore)

            val thrown = runCatching { preference.isOnboardingCompleted.first() }.exceptionOrNull()

            assertTrue(thrown is IllegalStateException)
            assertEquals("boom", thrown?.message)
        }

    @Test
    fun `cancellation during read is not swallowed as a recoverable failure`() =
        runTest(testDispatcher) {
            val failingDataStore = mockk<DataStore<Preferences>>()
            every { failingDataStore.data } returns flow { throw CancellationException("cancelled") }

            val preference = OnboardingPreference(failingDataStore)

            val thrown = runCatching { preference.isOnboardingCompleted.first() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
        }
}
