package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/**
 * JVM Desktop production engine for the shared NewsAPI [HttpClient].
 *
 * There is no Desktop composition root yet; this actual is exercised through
 * the Desktop test suite until the host wires the data layer explicitly.
 */
actual fun createHttpClient(config: NewsApiConfig): HttpClient =
    HttpClient(CIO) {
        configureNewsApiClient(config)
    }
