package pl.recipesforsoftware.signalbrief.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.room.Room
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pl.recipesforsoftware.signalbrief.data.local.NewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.data.remote.NewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingPreference
import pl.recipesforsoftware.signalbrief.ui.onboarding.onboardingDataStore
import pl.recipesforsoftware.signalbrief.ui.theme.ThemePreference
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AndroidGraphTest {
    @Test
    fun `production graph shares resources and preserves both preference stores`() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            val database = Room.inMemoryDatabaseBuilder(context, SignalBriefDatabase::class.java).build()
            val client = mockk<HttpClient>()
            val config = NewsApiConfig(apiKey = "test-only", baseUrl = "https://example.invalid/")
            var clientCount = 0
            var databaseCount = 0
            val application =
                koinApplication {
                    androidContext(context)
                    modules(
                        androidDataModule(config, { supplied ->
                            assertSame(config, supplied)
                            clientCount++
                            client
                        }, { supplied ->
                            assertSame(context, supplied)
                            databaseCount++
                            database
                        }),
                    )
                }
            try {
                val koin = application.koin
                val contracts =
                    listOf(
                        NewsRepository::class,
                        SavedArticlesRepository::class,
                        CollectionsRepository::class,
                        TopicMonitoringRepository::class,
                        NewsLocalDataSource::class,
                        NewsRemoteDataSource::class,
                        ThemePreference::class,
                        OnboardingPreference::class,
                    )
                contracts.forEach { type -> assertSame(koin.get(type), koin.get(type)) }
                assertSame(config, koin.get<NewsApiConfig>())
                assertSame(client, koin.get<HttpClient>())
                assertSame(database, koin.get<SignalBriefDatabase>())
                assertEquals(1, clientCount)
                assertEquals(1, databaseCount)
                val store = koin.get<DataStore<Preferences>>()
                assertSame(onboardingDataStore(context), store)
                val theme = koin.get<ThemePreference>()
                val onboarding = koin.get<OnboardingPreference>()
                assertFalse(theme.isDarkModeEnabled.first())
                assertFalse(onboarding.isOnboardingCompleted.first())
                theme.setDarkMode(true)
                assertFalse(onboarding.isOnboardingCompleted.first())
                onboarding.setOnboardingCompleted(true)
                assertTrue(ThemePreference(context).isDarkModeEnabled.first())
                assertTrue(OnboardingPreference(onboardingDataStore(context)).isOnboardingCompleted.first())
                assertEquals(true, store.data.first()[booleanPreferencesKey("onboarding_completed")])
                assertTrue(File(context.filesDir, "datastore/theme_preferences.preferences_pb").exists())
                assertTrue(File(context.filesDir, "datastore/onboarding_preferences.preferences_pb").exists())
            } finally {
                application.close()
                database.close()
            }
        }
}
