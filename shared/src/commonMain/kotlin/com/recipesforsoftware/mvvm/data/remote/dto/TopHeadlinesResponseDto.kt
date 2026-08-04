package com.recipesforsoftware.mvvm.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Remote transport shape of the NewsAPI top-headlines response envelope.
 */
@Serializable
data class TopHeadlinesResponseDto(
    val status: String? = null,
    val totalResults: Int? = null,
    val articles: List<ArticleDto>? = null,
)
