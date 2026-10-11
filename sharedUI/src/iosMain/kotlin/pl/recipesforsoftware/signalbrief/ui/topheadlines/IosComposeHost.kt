package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
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
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefApp
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefNavHost
import pl.recipesforsoftware.signalbrief.ui.app.defaultSignalBriefEntries
import pl.recipesforsoftware.signalbrief.ui.di.signalBriefViewModelModule
import pl.recipesforsoftware.signalbrief.ui.main.MainViewModel
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

private const val ONBOARDING_KEY = "pl.recipesforsoftware.signalbrief.onboarding.completed"

/**
 * iOS composition root, invoked from SwiftUI as `IosComposeHostKt.createIosComposeHost()`.
 *
 * Onboarding completion is read synchronously from [NSUserDefaults] before
 * Compose starts, so returning users never see an onboarding flash. The
 * isolated Koin application is created once and disposed with this host.
 */
fun createIosComposeHost(): UIViewController {
    val onboardingCompleted = readOnboardingCompleted()

    return ComposeUIViewController {
        val composition = remember { createIosComposition() }

        DisposableEffect(composition) {
            onDispose { composition.dispose() }
        }

        KoinIsolatedContext(context = composition.koinApplication) {
            SignalBriefTheme {
                var completed by remember { mutableStateOf(onboardingCompleted) }
                val mainViewModel: MainViewModel = koinViewModel()
                val savedArticleCount by mainViewModel.savedArticleCount.collectAsState(initial = 0)

                SignalBriefApp(onboardingCompleted = completed, onCompleteOnboarding = {
                    setOnboardingCompleted(true)
                    completed = true
                }) {
                    SignalBriefNavHost(savedArticleCount = savedArticleCount) { navigation ->
                        defaultSignalBriefEntries(navigation)
                    }
                }
            }
        }
    }
}

/**
 * Holds the single iOS composition root and its externally owned resources.
 *
 * [dispose] is idempotent and must be called when the owning composition root
 * is torn down.
 */
internal class IosComposition(
    internal val koinApplication: KoinApplication,
    private val resources: IosCompositionResources,
) {
    private var isDisposed = false

    fun dispose() {
        if (isDisposed) return
        isDisposed = true

        try {
            resources.dispose()
        } finally {
            koinApplication.close()
        }
    }
}

/**
 * Creates the single iOS composition graph.
 *
 * The returned composition owns the platform resources while Koin owns object
 * graph construction. Factory arguments keep construction isolated from the
 * app bundle and user storage in iOS tests.
 */
@Suppress("TooGenericExceptionCaught")
internal fun createIosComposition(
    apiKey: String,
    httpClientFactory: (NewsApiConfig) -> HttpClient = ::createHttpClient,
    databaseFactory: () -> SignalBriefDatabase = ::createSignalBriefDatabase,
): IosComposition {
    val config = NewsApiConfig(apiKey = apiKey, baseUrl = "https://newsapi.org/v2/")
    val resources = IosCompositionResources()
    val application =
        koinApplication {
            modules(
                iosModule(
                    config = config,
                    httpClientFactory = httpClientFactory,
                    databaseFactory = databaseFactory,
                    resources = resources,
                ),
                signalBriefViewModelModule,
            )
        }

    try {
        val koin = application.koin
        koin.get<NewsRepository>()
        koin.get<SavedArticlesRepository>()
        koin.get<CollectionsRepository>()
        koin.get<TopicMonitoringRepository>()
        return IosComposition(koinApplication = application, resources = resources)
    } catch (exception: Exception) {
        val constructionFailure = exception.unwrapKoinCreationExceptions()
        runCatching(application::close)
            .exceptionOrNull()
            ?.let(constructionFailure::addSuppressed)
        resources.disposeAfterFailedConstruction(constructionFailure)
    }
}

private fun iosModule(
    config: NewsApiConfig,
    httpClientFactory: (NewsApiConfig) -> HttpClient,
    databaseFactory: () -> SignalBriefDatabase,
    resources: IosCompositionResources,
): Module =
    module {
        single { config }
        single<HttpClient> {
            httpClientFactory(get()).also(resources::registerHttpClient)
        }
        single<SignalBriefDatabase> {
            databaseFactory().also(resources::registerDatabase)
        }
        single<NewsRemoteDataSource> { KtorNewsRemoteDataSource(get()) }
        single<NewsLocalDataSource> { RoomNewsLocalDataSource(get()) }
        single<NewsRepository> { OfflineFirstNewsRepository(get(), get()) }
        single<SavedArticlesRepository> { RoomSavedArticlesRepository(get()) }
        single<CollectionsRepository> { RoomCollectionsRepository(get()) }
        single<TopicMonitoringRepository> { RoomTopicMonitoringRepository(get()) }
    }

/** Owns resources created while resolving the iOS Koin graph. */
internal class IosCompositionResources(
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

private fun createIosComposition(): IosComposition {
    val apiKey =
        readNewsApiKeyFromBundle()
            ?: error(
                "NEWS_API_KEY is missing or empty. " +
                    "Configure iosApp/Secrets.xcconfig (copy Secrets.example.xcconfig) " +
                    "and rebuild the app.",
            )
    return createIosComposition(apiKey)
}

/**
 * Reads the NewsAPI key injected into Info.plist by the build via
 * `NEWS_API_KEY = $(NEWS_API_KEY)` from the git-ignored `Secrets.xcconfig`.
 */
private fun readNewsApiKeyFromBundle(): String? =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("NEWS_API_KEY") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

private fun readOnboardingCompleted(): Boolean = NSUserDefaults.standardUserDefaults.boolForKey(ONBOARDING_KEY)

private fun setOnboardingCompleted(completed: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(completed, ONBOARDING_KEY)
}
