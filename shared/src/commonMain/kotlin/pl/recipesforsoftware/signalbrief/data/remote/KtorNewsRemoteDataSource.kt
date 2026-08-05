package pl.recipesforsoftware.signalbrief.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pl.recipesforsoftware.signalbrief.data.remote.dto.TopHeadlinesResponseDto
import pl.recipesforsoftware.signalbrief.data.remote.mapper.toDomainArticles
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor-backed [NewsRemoteDataSource] shared by Android and iOS.
 *
 * Transport concerns stay inside this class: Ktor and serialization exceptions
 * are translated into [NewsFailure] so no raw transport exception reaches the
 * repository or the UI. The request path and parameters are built inline so the
 * full request can be asserted through the Ktor MockEngine in common tests. The
 * supplied [client] is owned by the composition root and is never closed here.
 */
class KtorNewsRemoteDataSource(
    private val client: HttpClient,
) : NewsRemoteDataSource {
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getTopHeadlines(country: String): Result<List<Article>> =
        try {
            val response =
                client.get("top-headlines") {
                    parameter("country", country)
                }
            val dto = response.body<TopHeadlinesResponseDto>()
            Result.success(dto.toDomainArticles())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toNewsFailure())
        }
}

private fun Throwable.toNewsFailure(): NewsFailure =
    when (this) {
        is ResponseException -> NewsFailure.Network
        is IOException -> NewsFailure.Network
        is ContentConvertException -> NewsFailure.InvalidData
        is SerializationException -> NewsFailure.InvalidData
        else -> NewsFailure.Unknown(this)
    }
