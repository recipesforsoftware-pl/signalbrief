package com.recipesforsoftware.mvvm.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the NewsAPI [HttpClient] backed by the platform engine of the current
 * target (Android engine on Android, Darwin engine on iOS).
 *
 * The actual engine selection lives in the platform source sets; all shared
 * client configuration (content negotiation, timeouts, response validation,
 * default base URL and API-key header) is applied through
 * [configureNewsApiClient] so Android and iOS behave identically.
 */
expect fun createHttpClient(config: NewsApiConfig): HttpClient

/**
 * Applies the shared NewsAPI client configuration.
 *
 * This is a deliberate test seam: common tests build a client around
 * `MockEngine` with the exact same configuration used in production.
 */
internal fun HttpClientConfig<*>.configureNewsApiClient(config: NewsApiConfig) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }

    defaultRequest {
        url(config.baseUrl)
        header(API_KEY_HEADER, config.apiKey)
    }
}

private const val API_KEY_HEADER = "X-Api-Key"

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val SOCKET_TIMEOUT_MILLIS = 10_000L
