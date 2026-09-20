package pl.recipesforsoftware.signalbrief.desktop

import io.ktor.client.HttpClient
import pl.recipesforsoftware.signalbrief.data.local.RoomNewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createSignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.remote.KtorNewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
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
    private val resources: DesktopCompositionResources,
) {
    fun dispose() {
        resources.dispose()
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
    val client = httpClientFactory(config)
    var database: SignalBriefDatabase? = null

    try {
        val createdDatabase = databaseFactory(databasePath)
        database = createdDatabase
        val remoteDataSource = KtorNewsRemoteDataSource(client)
        val localDataSource = RoomNewsLocalDataSource(createdDatabase)

        return DesktopComposition(
            newsRepository =
                OfflineFirstNewsRepository(
                    remoteDataSource = remoteDataSource,
                    localDataSource = localDataSource,
                ),
            savedArticlesRepository = RoomSavedArticlesRepository(createdDatabase),
            collectionsRepository = RoomCollectionsRepository(createdDatabase),
            topicMonitoringRepository = RoomTopicMonitoringRepository(createdDatabase),
            resources =
                DesktopCompositionResources(
                    closeHttpClient = client::close,
                    closeDatabase = createdDatabase::close,
                ),
        )
    } catch (exception: Exception) {
        closeResourcesAfterFailedConstruction(
            database = database,
            client = client,
            constructionFailure = exception,
        )
    }
}

/** Owns the two resources created by [createDesktopComposition]. */
internal class DesktopCompositionResources(
    private val closeHttpClient: () -> Unit,
    private val closeDatabase: () -> Unit,
) {
    private var isDisposed = false

    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        try {
            closeHttpClient()
        } finally {
            closeDatabase()
        }
    }
}

private fun closeResourcesAfterFailedConstruction(
    database: SignalBriefDatabase?,
    client: HttpClient,
    constructionFailure: Exception,
): Nothing {
    runCatching { database?.close() }
        .exceptionOrNull()
        ?.let(constructionFailure::addSuppressed)
    runCatching(client::close)
        .exceptionOrNull()
        ?.let(constructionFailure::addSuppressed)
    throw constructionFailure
}

private const val NEWS_API_BASE_URL = "https://newsapi.org/v2/"
