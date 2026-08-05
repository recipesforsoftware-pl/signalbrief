package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android

/**
 * Android production engine for the shared NewsAPI [HttpClient].
 */
actual fun createHttpClient(config: NewsApiConfig): HttpClient =
    HttpClient(Android) {
        configureNewsApiClient(config)
    }
