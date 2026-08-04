package com.recipesforsoftware.mvvm.data.repository

import com.google.gson.JsonParseException
import com.recipesforsoftware.mvvm.data.api.NetworkService
import com.recipesforsoftware.mvvm.data.remote.mapper.toDomainArticles
import com.recipesforsoftware.mvvm.domain.failure.NewsFailure
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retrofit-backed [NewsRepository] implementation.
 *
 * Transport concerns stay inside this class: Retrofit/Gson exceptions are
 * translated into [NewsFailure] so no raw transport exception reaches the UI.
 */
@Singleton
class TopHeadlineRepository
    @Inject
    constructor(
        private val networkService: NetworkService,
    ) : NewsRepository {
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        override suspend fun getTopHeadlines(country: String): Result<List<Article>> =
            try {
                val response = networkService.getTopHeadlines(country)
                Result.success(response.toDomainArticles())
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                Result.failure(NewsFailure.Network)
            } catch (e: IOException) {
                Result.failure(NewsFailure.Network)
            } catch (e: JsonParseException) {
                Result.failure(NewsFailure.InvalidData)
            } catch (e: Exception) {
                Result.failure(NewsFailure.Unknown(e))
            }
    }
