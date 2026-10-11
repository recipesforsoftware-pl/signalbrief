package pl.recipesforsoftware.signalbrief.desktop

import io.ktor.client.HttpClient
import kotlinx.coroutines.isActive
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.main.MainViewModel
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopCompositionTest {
    @Test
    fun disposeClosesBothResourcesOnlyOnce() {
        var closedHttpClients = 0
        var closedDatabases = 0
        val resources =
            DesktopCompositionResources(
                closeHttpClient = { closedHttpClients += 1 },
                closeDatabase = { closedDatabases += 1 },
            )

        resources.dispose()
        resources.dispose()

        assertEquals(1, closedHttpClients)
        assertEquals(1, closedDatabases)
    }

    @Test
    fun disposeClosesDatabaseWhenHttpClientCloseFails() {
        var closedDatabases = 0
        val resources =
            DesktopCompositionResources(
                closeHttpClient = { error("HTTP close failed") },
                closeDatabase = { closedDatabases += 1 },
            )

        assertFailsWith<IllegalStateException> { resources.dispose() }

        assertEquals(1, closedDatabases)
    }

    @Test
    fun factoryClosesClientWhenDatabaseConstructionFails() {
        lateinit var client: HttpClient
        val nestedCause = IOException("Nested database failure")
        val factoryFailure = IllegalStateException("Database construction failed", nestedCause)

        val exception =
            assertFailsWith<IllegalStateException> {
                createDesktopComposition(
                    apiKey = "fake-test-key",
                    databasePath = "unused.db",
                    httpClientFactory = { config -> createHttpClient(config).also { client = it } },
                    databaseFactory = { throw factoryFailure },
                )
            }

        assertSame(factoryFailure, exception)
        assertEquals("Database construction failed", exception.message)
        assertSame(nestedCause, exception.cause)
        assertFalse(client.coroutineContext.isActive)
    }

    @Test
    fun factoryBuildsRepositoriesFromDesktopDataGraph() {
        val databasePath = kotlin.io.path.createTempFile("signalbrief-desktop-composition", ".db")

        val composition =
            createDesktopComposition(
                apiKey = "fake-test-key",
                databasePath = databasePath.toString(),
            )

        try {
            val koin = composition.koinApplication.koin
            assertNotNull(koin.get<NewsRepository>())
            assertNotNull(koin.get<SavedArticlesRepository>())
            assertNotNull(koin.get<CollectionsRepository>())
            assertNotNull(koin.get<TopicMonitoringRepository>())
            assertNotNull(koin.get<MainViewModel>())
        } finally {
            composition.dispose()
            Files.deleteIfExists(databasePath)
        }
    }

    @Test
    fun factoryCreatesIndependentKoinApplications() {
        val firstDatabasePath = kotlin.io.path.createTempFile("signalbrief-desktop-first", ".db")
        val secondDatabasePath = kotlin.io.path.createTempFile("signalbrief-desktop-second", ".db")
        val first = createDesktopComposition("first-key", firstDatabasePath.toString())
        val second = createDesktopComposition("second-key", secondDatabasePath.toString())

        try {
            assertNotSame(
                first.koinApplication.koin.get<NewsRepository>(),
                second.koinApplication.koin.get<NewsRepository>(),
            )
        } finally {
            first.dispose()
            second.dispose()
            Files.deleteIfExists(firstDatabasePath)
            Files.deleteIfExists(secondDatabasePath)
        }
    }
}
