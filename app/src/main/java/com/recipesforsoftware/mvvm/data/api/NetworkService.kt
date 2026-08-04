package com.recipesforsoftware.mvvm.data.api

import com.recipesforsoftware.mvvm.data.remote.dto.TopHeadlinesResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface NetworkService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("country") country: String,
    ): TopHeadlinesResponseDto
}
