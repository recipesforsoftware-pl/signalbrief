package com.recipesforsoftware.mvvm.data.model

import com.google.gson.annotations.SerializedName

data class Article(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("urlToImage")
    val imageUrl: String? = null,
    @SerializedName("source")
    val source: Source? = null,
)
