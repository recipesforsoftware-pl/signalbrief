package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.io.IOException
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabaseConstructor
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class IosCompositionTest {
    @Test
    fun resourceOwnerClosesBothResourcesOnlyOnce() {
        var closedHttpClients = 0
        var closedDatabases = 0
        val resources =
            IosCompositionResources(
                closeHttpClient = { closedHttpClients += 1 },
                closeDatabase = { closedDatabases += 1 },
            )

        resources.dispose()
        resources.dispose()

        assertEquals(1, closedHttpClients)
        assertEquals(1, closedDatabases)
    }

    @Test
    fun resourceOwnerClosesDatabaseWhenHttpClientCloseFails() {
        var closedDatabases = 0
        val resources =
            IosCompositionResources(
                closeHttpClient = { error("HTTP close failed") },
                closeDatabase = { closedDatabases += 1 },
            )

        assertFailsWith<IllegalStateException> { resources.dispose() }

        assertEquals(1, closedDatabases)
    }

    @Test
    fun factoryClosesClientAndPreservesFailureWhenDatabaseConstructionFails() {
        lateinit var client: HttpClient
        val nestedCause = IOException("Nested database failure")
        val factoryFailure = IllegalStateException("Database construction failed", nestedCause)

        val exception =
            assertFailsWith<IllegalStateException> {
                createIosComposition(
                    apiKey = "fake-test-key",
                    httpClientFactory = { config -> createHttpClient(config).also { client = it } },
                    databaseFactory = { throw factoryFailure },
                )
            }

        assertSame(factoryFailure, exception)
        assertSame(nestedCause, exception.cause)
        assertFalse(client.coroutineContext.isActive)
    }

    @Test
    fun factoryBuildsAllRepositoryContractsFromTheIosGraph() {
        val composition =
            createIosComposition(
                apiKey = "fake-test-key",
                databaseFactory = ::createTemporaryTestDatabase,
            )

        try {
            assertNotNull(composition.newsRepository)
            assertNotNull(composition.savedArticlesRepository)
            assertNotNull(composition.collectionsRepository)
            assertNotNull(composition.topicMonitoringRepository)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun factoryCreatesIndependentKoinApplications() {
        val first =
            createIosComposition(
                apiKey = "first-key",
                databaseFactory = ::createTemporaryTestDatabase,
            )
        val second =
            createIosComposition(
                apiKey = "second-key",
                databaseFactory = ::createTemporaryTestDatabase,
            )

        try {
            assertNotSame(first.newsRepository, second.newsRepository)
        } finally {
            first.dispose()
            second.dispose()
        }
    }

    @Test
    fun compositionDisposeIsIdempotent() {
        val composition =
            createIosComposition(
                apiKey = "fake-test-key",
                databaseFactory = ::createTemporaryTestDatabase,
            )

        composition.dispose()
        composition.dispose()
    }
}

private fun createTemporaryTestDatabase(): SignalBriefDatabase {
    val databasePath = "${NSTemporaryDirectory()}/signalbrief-ui-test-${NSUUID.UUID().UUIDString}.db"
    return Room
        .databaseBuilder<SignalBriefDatabase>(
            name = databasePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
