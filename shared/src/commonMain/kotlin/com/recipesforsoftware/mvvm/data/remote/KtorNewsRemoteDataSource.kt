package com.recipesforsoftware.mvvm.data.remote

import com.recipesforsoftware.mvvm.data.remote.dto.TopHeadlinesResponseDto
import com.recipesforsoftware.mvvm.data.remote.mapper.toDomainArticles
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
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
