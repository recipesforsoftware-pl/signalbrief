package pl.recipesforsoftware.signalbrief.desktop

import io.ktor.client.HttpClient
import kotlinx.coroutines.isActive
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

        val exception =
            assertFailsWith<IllegalStateException> {
                createDesktopComposition(
                    apiKey = "fake-test-key",
                    databasePath = "unused.db",
                    httpClientFactory = { config -> createHttpClient(config).also { client = it } },
                    databaseFactory = { error("Database construction failed") },
                )
            }

        assertTrue(exception.message.orEmpty().contains("Database construction failed"))
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
}
