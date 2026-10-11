package pl.recipesforsoftware.signalbrief.ui.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentViewModel
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsViewModel
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefViewModel
import pl.recipesforsoftware.signalbrief.ui.main.MainViewModel
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel

val signalBriefViewModelModule =
    module {
        viewModel { MainViewModel(get()) }
        viewModel { TopHeadlinesViewModel(get(), get()) }
        viewModel { SavedArticlesViewModel(get()) }
        viewModel { CollectionsViewModel(get()) }
        viewModel { (collection: Collection) -> CollectionDetailsViewModel(collection, get()) }
        viewModel { DailyBriefViewModel(get(), get()) }
        viewModel { (query: String) -> SearchViewModel(get(), get(), query) }
        viewModel { TopicMonitoringViewModel(get(), get()) }
        viewModel { (topic: MonitoredTopic) -> TopicMatchesViewModel(topic, get(), get()) }
        viewModel { (article: Article) -> ArticleDetailsViewModel(get(), article) }
        viewModel { (article: Article) -> ArticleCollectionAssignmentViewModel(get(), article) }
        viewModel { SettingsViewModel(get()) }
    }
