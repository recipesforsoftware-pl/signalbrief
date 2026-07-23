package com.recipesforsoftware.mvvm.data.repository

import com.recipesforsoftware.mvvm.data.api.NetworkService
import com.recipesforsoftware.mvvm.data.model.Article
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopHeadlineRepository @Inject constructor(
    private val networkService: NetworkService
) {

    suspend fun getTopHeadlines(country: String): Result<List<Article>> {
        return try {
            val response = networkService.getTopHeadlines(country)
            val articles = response.articles.orEmpty()
            Result.success(articles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
