package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS production engine for the shared NewsAPI [HttpClient].
 *
 * There is no iOS application yet; this actual is exercised through the iOS
 * simulator framework link task and the common test suites.
 */
actual fun createHttpClient(config: NewsApiConfig): HttpClient =
    HttpClient(Darwin) {
        configureNewsApiClient(config)
    }
