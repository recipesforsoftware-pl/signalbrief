package com.recipesforsoftware.mvvm.data.remote.mapper

import com.recipesforsoftware.mvvm.data.remote.dto.ArticleDto
import com.recipesforsoftware.mvvm.data.remote.dto.SourceDto
import com.recipesforsoftware.mvvm.data.remote.dto.TopHeadlinesResponseDto
import com.recipesforsoftware.mvvm.domain.model.Article
import com.recipesforsoftware.mvvm.domain.model.Source

/**
 * Deterministic DTO-to-domain mapping for top headlines.
 *
 * Nullability policy:
 * - optional display fields (title, description, imageUrl, source) map 1:1 and
 *   keep their nulls — no placeholder values are ever invented;
 * - an article whose [ArticleDto.url] is null or blank is not a usable headline
 *   and is dropped (mapped to `null` and filtered out).
 */
internal fun ArticleDto.toDomain(): Article? {
    if (url.isNullOrBlank()) {
        return null
    }
    return Article(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = source?.toDomain(),
    )
}

internal fun SourceDto.toDomain(): Source =
    Source(
        id = id,
        name = name,
    )

/**
 * Maps a full response, skipping articles that cannot map to a valid domain
 * model. A missing `articles` list maps to an empty list.
 */
internal fun TopHeadlinesResponseDto.toDomainArticles(): List<Article> = articles.orEmpty().mapNotNull { it.toDomain() }
