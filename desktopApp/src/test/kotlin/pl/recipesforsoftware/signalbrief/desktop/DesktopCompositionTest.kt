package pl.recipesforsoftware.signalbrief.desktop

import io.ktor.client.HttpClient
import kotlinx.coroutines.isActive
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
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
            assertNotNull(composition.newsRepository)
            assertNotNull(composition.savedArticlesRepository)
            assertNotNull(composition.collectionsRepository)
            assertNotNull(composition.topicMonitoringRepository)
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
            assertNotSame(first.newsRepository, second.newsRepository)
        } finally {
            first.dispose()
            second.dispose()
            Files.deleteIfExists(firstDatabasePath)
            Files.deleteIfExists(secondDatabasePath)
        }
    }
}
