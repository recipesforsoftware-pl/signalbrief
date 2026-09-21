package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavioral tests for [KtorNewsRemoteDataSource] driven by the Ktor
 * [MockEngine].
 *
 * The client is configured with the exact same shared configuration used in
 * production (content negotiation, timeouts, validation, base URL and API-key
 * header), so the tests exercise the real request/response pipeline.
 */
class KtorNewsRemoteDataSourceTest {
    private val config =
        NewsApiConfig(
            apiKey = FAKE_API_KEY,
            baseUrl = "https://newsapi.example/v2/",
        )

    private fun dataSourceWith(handler: MockRequestHandler): KtorNewsRemoteDataSource {
        val client =
            HttpClient(MockEngine) {
                engine {
                    addHandler(handler)
                }
                configureNewsApiClient(config)
            }
        return KtorNewsRemoteDataSource(client)
    }

    @Test
    fun successfulResponseIsMappedToDomainArticles() =
        runTest {
            var request: HttpRequestData? = null
            val dataSource =
                dataSourceWith { captured ->
                    request = captured
                    respond(SUCCESSFUL_RESPONSE, headers = JSON_HEADERS)
                }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            val article = result.getOrNull()?.single()
            assertEquals("Headline", article?.title)
            assertEquals("Summary", article?.description)
            assertEquals("https://example.com/a", article?.url)
            assertEquals("https://example.com/a.jpg", article?.imageUrl)
            assertEquals("src", article?.source?.id)
            assertEquals("Source", article?.source?.name)

            val captured = requireNotNull(request)
            assertEquals("/v2/top-headlines", captured.url.encodedPath)
            assertEquals("us", captured.url.parameters["country"])
            assertEquals(FAKE_API_KEY, captured.headers[API_KEY_HEADER])
        }

    @Test
    fun unknownJsonFieldsAreIgnored() =
        runTest {
            val dataSource = dataSourceWith { respond(UNKNOWN_FIELDS_RESPONSE, headers = JSON_HEADERS) }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertEquals("Headline", result.getOrNull()?.single()?.title)
        }

    @Test
    fun nullArticlesListMapsToAnEmptyList() =
        runTest {
            val dataSource = dataSourceWith { respond(NULL_ARTICLES_RESPONSE, headers = JSON_HEADERS) }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
        }

    @Test
    fun articlesWithMissingOrBlankUrlAreDropped() =
        runTest {
            val dataSource = dataSourceWith { respond(DROPPED_ARTICLES_RESPONSE, headers = JSON_HEADERS) }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isEmpty() == true)
        }

    @Test
    fun non2xxResponseMapsToNetworkFailure() =
        runTest {
            val dataSource =
                dataSourceWith {
                    respond(
                        content = "Internal Server Error",
                        status = HttpStatusCode.InternalServerError,
                    )
                }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertEquals(NewsFailure.Network, result.exceptionOrNull())
        }

    @Test
    fun transportExceptionMapsToNetworkFailure() =
        runTest {
            val dataSource = dataSourceWith { throw IOException("connection reset") }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertEquals(NewsFailure.Network, result.exceptionOrNull())
            assertNull(result.exceptionOrNull()?.message)
        }

    @Test
    fun malformedJsonMapsToInvalidDataFailure() =
        runTest {
            val dataSource = dataSourceWith { respond(MALFORMED_RESPONSE, headers = JSON_HEADERS) }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            assertEquals(NewsFailure.InvalidData, result.exceptionOrNull())
        }

    @Test
    fun unexpectedFailureMapsToUnknownFailurePreservingCause() =
        runTest {
            val boom = IllegalStateException("boom")
            val dataSource = dataSourceWith { throw boom }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            val failure = result.exceptionOrNull()
            assertTrue(failure is NewsFailure.Unknown)
            val cause = failure.cause
            assertTrue(cause is IllegalStateException)
            assertEquals("boom", cause.message)
        }

    @Test
    fun cancellationIsRethrownInsteadOfReportedAsFailure() =
        runTest {
            val dataSource = dataSourceWith { throw CancellationException("cancelled") }

            var thrown: Throwable? = null
            try {
                dataSource.getTopHeadlines("us")
            } catch (e: CancellationException) {
                thrown = e
            }

            assertTrue(thrown is CancellationException)
        }

    @Test
    fun failureMessagesNeverExposeTheApiKey() =
        runTest {
            val dataSource =
                dataSourceWith {
                    throw IOException("connection reset for $FAKE_API_KEY")
                }

            val result = dataSource.getTopHeadlines("us")

            assertTrue(result.isFailure)
            val failureMessage = result.exceptionOrNull()?.message
            assertNull(failureMessage)
            assertFalse(failureMessage?.contains(FAKE_API_KEY) == true)
        }

    @Test
    fun requestCarriesTheInjectedApiKeyHeader() =
        runTest {
            var request: HttpRequestData? = null
            val dataSource =
                dataSourceWith { captured ->
                    request = captured
                    respond(SUCCESSFUL_RESPONSE, headers = JSON_HEADERS)
                }

            dataSource.getTopHeadlines("us")

            assertEquals(FAKE_API_KEY, request?.headers?.get(API_KEY_HEADER))
        }

    private companion object {
        const val FAKE_API_KEY = "test-api-key"
        const val API_KEY_HEADER = "X-Api-Key"

        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val SUCCESSFUL_RESPONSE =
            """
            {
              "status": "ok",
              "totalResults": 1,
              "articles": [
                {
                  "title": "Headline",
                  "description": "Summary",
                  "url": "https://example.com/a",
                  "urlToImage": "https://example.com/a.jpg",
                  "source": { "id": "src", "name": "Source" }
                }
              ]
            }
            """.trimIndent()

        val UNKNOWN_FIELDS_RESPONSE =
            """
            {
              "status": "ok",
              "totalResults": 1,
              "unexpectedEnvelopeField": true,
              "articles": [
                {
                  "title": "Headline",
                  "description": "Summary",
                  "url": "https://example.com/a",
                  "urlToImage": "https://example.com/a.jpg",
                  "unexpectedArticleField": "ignored",
                  "source": { "id": "src", "name": "Source", "unexpectedSourceField": 42 }
                }
              ]
            }
            """.trimIndent()

        val NULL_ARTICLES_RESPONSE =
            """
            {
              "status": "ok",
              "totalResults": 0,
              "articles": null
            }
            """.trimIndent()

        val DROPPED_ARTICLES_RESPONSE =
            """
            {
              "status": "ok",
              "totalResults": 2,
              "articles": [
                { "title": "No url", "url": null },
                { "title": "Blank url", "url": "   " }
              ]
            }
            """.trimIndent()

        const val MALFORMED_RESPONSE = "{ this is not valid json"
    }
}
