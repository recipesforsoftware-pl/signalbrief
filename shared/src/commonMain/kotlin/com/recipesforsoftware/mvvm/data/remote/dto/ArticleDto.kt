package com.recipesforsoftware.mvvm.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Remote transport shape of a single NewsAPI article.
 *
 * Tolerant parsing: every field is nullable because the remote contract does
 * not guarantee them. Unknown JSON keys are ignored by the shared JSON
 * configuration.
 */
@Serializable
data class ArticleDto(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    @SerialName("urlToImage")
    val imageUrl: String? = null,
    val source: SourceDto? = null,
)
