package com.recipesforsoftware.mvvm.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Remote transport shape of the NewsAPI top-headlines response envelope.
 */
data class TopHeadlinesResponseDto(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("totalResults")
    val totalResults: Int? = null,
    @SerializedName("articles")
    val articles: List<ArticleDto>? = null,
)
