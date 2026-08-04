package com.recipesforsoftware.mvvm.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Remote transport shape of a single NewsAPI article.
 *
 * Tolerant parsing: every field is nullable because the remote contract does
 * not guarantee them. Gson annotations are confined to the data layer.
 */
data class ArticleDto(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("urlToImage")
    val imageUrl: String? = null,
    @SerializedName("source")
    val source: SourceDto? = null,
)
