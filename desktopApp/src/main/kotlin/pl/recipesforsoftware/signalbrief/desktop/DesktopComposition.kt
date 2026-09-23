package pl.recipesforsoftware.signalbrief.desktop

import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.error.InstanceCreationException
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
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

/**
 * Desktop-owned data dependencies for the future application host.
 *
 * Only domain repository contracts leave this composition boundary. The owner
 * must call [dispose] when the Desktop application shuts down.
 */
internal class DesktopComposition(
    val newsRepository: NewsRepository,
    val savedArticlesRepository: SavedArticlesRepository,
    val collectionsRepository: CollectionsRepository,
    val topicMonitoringRepository: TopicMonitoringRepository,
    private val koinApplication: KoinApplication,
    private val resources: DesktopCompositionResources,
) {
    fun dispose() {
        try {
            resources.dispose()
        } finally {
            koinApplication.close()
        }
    }
}

/**
 * Builds the Desktop data graph with its platform-specific HTTP client and
 * Room database.
 *
 * The optional factories keep construction failure testable without a real
 * NewsAPI request or a user directory.
 */
@Suppress("TooGenericExceptionCaught")
internal fun createDesktopComposition(
    apiKey: String,
    databasePath: String,
    httpClientFactory: (NewsApiConfig) -> HttpClient = ::createHttpClient,
    databaseFactory: (String) -> SignalBriefDatabase = ::createSignalBriefDatabase,
): DesktopComposition {
    val config = NewsApiConfig(apiKey = apiKey, baseUrl = NEWS_API_BASE_URL)
    val resources = DesktopCompositionResources()
    val application =
        koinApplication {
            modules(
                desktopModule(
                    config = config,
                    databasePath = databasePath,
                    httpClientFactory = httpClientFactory,
                    databaseFactory = databaseFactory,
                    resources = resources,
                ),
            )
        }

    try {
        val koin = application.koin

        return DesktopComposition(
            newsRepository = koin.get(),
            savedArticlesRepository = koin.get(),
            collectionsRepository = koin.get(),
            topicMonitoringRepository = koin.get(),
            koinApplication = application,
            resources = resources,
        )
    } catch (exception: Exception) {
        val constructionFailure = exception.unwrapKoinCreationExceptions()
        runCatching(application::close)
            .exceptionOrNull()
            ?.let(constructionFailure::addSuppressed)
        resources.disposeAfterFailedConstruction(constructionFailure)
    }
}

private fun desktopModule(
    config: NewsApiConfig,
    databasePath: String,
    httpClientFactory: (NewsApiConfig) -> HttpClient,
    databaseFactory: (String) -> SignalBriefDatabase,
    resources: DesktopCompositionResources,
): Module =
    module {
        single { config }
        single<HttpClient> {
            httpClientFactory(get()).also(resources::registerHttpClient)
        }
        single<SignalBriefDatabase> {
            databaseFactory(databasePath).also(resources::registerDatabase)
        }
        single<NewsRemoteDataSource> { KtorNewsRemoteDataSource(get()) }
        single<NewsLocalDataSource> { RoomNewsLocalDataSource(get()) }
        single<NewsRepository> { OfflineFirstNewsRepository(get(), get()) }
        single<SavedArticlesRepository> { RoomSavedArticlesRepository(get()) }
        single<CollectionsRepository> { RoomCollectionsRepository(get()) }
        single<TopicMonitoringRepository> { RoomTopicMonitoringRepository(get()) }
    }

/** Owns the two resources created by [createDesktopComposition]. */
internal class DesktopCompositionResources(
    private var closeHttpClient: (() -> Unit)? = null,
    private var closeDatabase: (() -> Unit)? = null,
) {
    private var isDisposed = false

    fun registerHttpClient(client: HttpClient) {
        check(closeHttpClient == null) { "HTTP client is already registered." }
        closeHttpClient = client::close
    }

    fun registerDatabase(database: SignalBriefDatabase) {
        check(closeDatabase == null) { "Database is already registered." }
        closeDatabase = database::close
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        try {
            closeHttpClient?.invoke()
        } finally {
            closeDatabase?.invoke()
        }
    }

    fun disposeAfterFailedConstruction(constructionFailure: Exception): Nothing {
        if (!isDisposed) {
            isDisposed = true
            runCatching { closeDatabase?.invoke() }
                .exceptionOrNull()
                ?.let(constructionFailure::addSuppressed)
            runCatching { closeHttpClient?.invoke() }
                .exceptionOrNull()
                ?.let(constructionFailure::addSuppressed)
        }
        throw constructionFailure
    }
}

private fun Exception.unwrapKoinCreationExceptions(): Exception {
    var current = this
    while (current is InstanceCreationException && current.cause is Exception) {
        current = current.cause as Exception
    }
    return current
}

private const val NEWS_API_BASE_URL = "https://newsapi.org/v2/"
