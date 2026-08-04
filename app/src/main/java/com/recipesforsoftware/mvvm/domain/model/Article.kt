package com.recipesforsoftware.mvvm.domain.model

/**
 * Framework-independent news article used by the domain and UI layers.
 *
 * [url] is always present: a headline that cannot be opened is not a usable
 * article and is dropped during DTO-to-domain mapping. Optional display fields
 * stay nullable and are never filled with placeholder values.
 */
data class Article(
    val title: String?,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val source: Source?,
)
