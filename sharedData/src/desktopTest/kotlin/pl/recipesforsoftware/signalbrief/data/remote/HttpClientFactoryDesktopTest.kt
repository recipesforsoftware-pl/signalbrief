package pl.recipesforsoftware.signalbrief.data.remote

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Verifies that the production JVM Desktop [createHttpClient] actual can build a
 * CIO-backed client. The test performs no HTTP request and relies on no external
 * service; it only proves the engine is constructible on Desktop.
 */
class HttpClientFactoryDesktopTest {
    @Test
    fun createHttpClientBuildsACioBackedClient() {
        val client =
            createHttpClient(
                NewsApiConfig(
                    apiKey = "fake-desktop-test-key",
                    baseUrl = "https://example.invalid",
                ),
            )

        try {
            assertNotNull(client.engine)
        } finally {
            client.close()
        }
    }
}
