package com.recipesforsoftware.mvvm.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Remote transport shape of the publisher information nested in an article.
 */
data class SourceDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
)
