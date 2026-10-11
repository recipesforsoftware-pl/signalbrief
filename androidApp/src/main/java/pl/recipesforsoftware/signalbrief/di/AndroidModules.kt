package pl.recipesforsoftware.signalbrief.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.recipesforsoftware.signalbrief.data.local.NewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.RoomNewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createSignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.remote.KtorNewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.data.remote.NewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import pl.recipesforsoftware.signalbrief.data.repository.OfflineFirstNewsRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomCollectionsRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomSavedArticlesRepository
import pl.recipesforsoftware.signalbrief.data.repository.RoomTopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingPreference
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.onboarding.onboardingDataStore
import pl.recipesforsoftware.signalbrief.ui.theme.ThemePreference
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel

internal fun androidDataModule(
    config: NewsApiConfig,
    httpClientFactory: (NewsApiConfig) -> HttpClient = ::createHttpClient,
    databaseFactory: (Context) -> SignalBriefDatabase = ::createSignalBriefDatabase,
) = module {
    single { config }
    single { httpClientFactory(get()) }
    single { databaseFactory(androidContext()) }
    single<NewsRemoteDataSource> { KtorNewsRemoteDataSource(get()) }
    single<NewsLocalDataSource> { RoomNewsLocalDataSource(get()) }
    single<NewsRepository> { OfflineFirstNewsRepository(get(), get()) }
    single<SavedArticlesRepository> { RoomSavedArticlesRepository(get()) }
    single<CollectionsRepository> { RoomCollectionsRepository(get()) }
    single<TopicMonitoringRepository> { RoomTopicMonitoringRepository(get()) }
    single { ThemePreference(androidContext()) }
    single<DataStore<Preferences>> { onboardingDataStore(androidContext()) }
    single { OnboardingPreference(get()) }
}

internal val androidViewModelModule =
    module {
        viewModel { ThemeViewModel(get()) }
        viewModel { OnboardingViewModel(get()) }
    }
