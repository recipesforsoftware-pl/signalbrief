@file:OptIn(ExperimentalWasmJsInterop::class)

package pl.recipesforsoftware.signalbrief.web

import kotlinx.browser.window
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.Promise
import kotlin.js.asJsException
import kotlin.js.unsafeCast

internal class WebNewsRepository(
    private val loader: suspend (String) -> List<Article> = ::fetchHeadlines,
) : NewsRepository {
    private val cachedArticles = MutableStateFlow<List<Article>>(emptyList())

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> =
        runCatching {
            loader(country)
        }.fold(
            onSuccess = { articles ->
                cachedArticles.value = articles

                Result.success(
                    TopHeadlinesFeed(
                        articles = articles,
                        source = FeedSource.NETWORK,
                    ),
                )
            },
            onFailure = { failure ->
                when (failure) {
                    is CancellationException -> throw failure
                    is NewsFailure -> Result.failure(failure)
                    else -> Result.failure(NewsFailure.Unknown(failure))
                }
            },
        )

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cachedArticles
}

private suspend fun fetchHeadlines(country: String): List<Article> {
    val normalizedCountry =
        country
            .lowercase()
            .takeIf { value ->
                value.length == 2 && value.all(Char::isLetter)
            }
            ?: DEFAULT_COUNTRY

    val response =
        runCatching {
            window
                .fetch("/api/headlines?country=$normalizedCountry")
                .awaitValue()
        }.getOrElse(::throwNetworkFailure)

    if (!response.ok) {
        throw NewsFailure.Network
    }

    val json =
        runCatching {
            response
                .json()
                .awaitValue()
        }.getOrElse(::throwInvalidDataFailure)

    val articles =
        runCatching {
            mapArticles(json)
        }.getOrElse(::throwInvalidDataFailure)

    return articles ?: throw NewsFailure.InvalidData
}

private fun mapArticles(json: JsAny?): List<Article>? {
    val articles =
        json
            ?.unsafeCast<HeadlinesPayload>()
            ?.articles

    return articles
        ?.toList()
        ?.mapNotNull { article ->
            article.url
                ?.takeIf(String::isNotBlank)
                ?.let { url ->
                    Article(
                        title = article.title,
                        description = article.description,
                        url = url,
                        imageUrl = article.imageUrl,
                        source =
                            article.sourceName?.let { sourceName ->
                                Source(
                                    id = null,
                                    name = sourceName,
                                )
                            },
                    )
                }
        }
}

private fun throwNetworkFailure(failure: Throwable): Nothing {
    if (failure is CancellationException) {
        throw failure
    }

    throw NewsFailure.Network
}

private fun throwInvalidDataFailure(failure: Throwable): Nothing {
    if (failure is CancellationException) {
        throw failure
    }

    throw NewsFailure.InvalidData
}

private suspend fun <T : JsAny?> Promise<T>.awaitValue(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resumeWith(Result.success(value))
                null
            },
            onRejected = { reason ->
                continuation.resumeWithException(reason.asJsException())
                null
            },
        )
    }

private external interface HeadlinesPayload : JsAny {
    val articles: JsArray<HeadlinePayload>?
}

private external interface HeadlinePayload : JsAny {
    val title: String?
    val description: String?
    val url: String?
    val imageUrl: String?
    val sourceName: String?
}

private const val DEFAULT_COUNTRY = "us"
