package pl.recipesforsoftware.signalbrief.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefAppHost
import pl.recipesforsoftware.signalbrief.ui.di.signalBriefViewModelModule
import pl.recipesforsoftware.signalbrief.ui.topheadlines.SignalBriefTheme

private val webDataModule =
    module {
        single<NewsRepository> { WebNewsRepository() }
        single<SavedArticlesRepository> { WebSavedArticlesRepository() }
        single<CollectionsRepository> { WebCollectionsRepository() }
        single<TopicMonitoringRepository> { WebTopicMonitoringRepository() }
    }

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        KoinApplication(
            configuration =
                koinConfiguration {
                    modules(webDataModule, signalBriefViewModelModule)
                },
        ) {
            SignalBriefTheme {
                SignalBriefAppHost()
            }
        }
    }
}
