package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * JVM Desktop production engine for the shared NewsAPI [HttpClient].
 *
 * `DesktopComposition` owns the resulting client and closes it when the Desktop
 * application lifetime ends. This factory only constructs the client.
 */
actual fun createHttpClient(config: NewsApiConfig): HttpClient =
    HttpClient(CIO) {
        configureNewsApiClient(config)
    }
