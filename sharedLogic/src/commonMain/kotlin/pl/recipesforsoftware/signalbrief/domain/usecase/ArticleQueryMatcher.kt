package pl.recipesforsoftware.signalbrief.domain.usecase

import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Deterministic, framework-independent matcher for an [Article] against a user
 * query.
 *
 * A query matches an article when the trimmed query occurs as a case-insensitive
 * substring in at least one of the article's user-visible text fields: [title],
 * [description], or [source] name. URLs and image URLs are excluded so a query
 * such as `example.com` never matches merely because it appears in an article
 * URL. A blank query matches nothing.
 *
 * Matching preserves the caller's input order: this object never reorders,
 * ranks, scores, or deduplicates the articles passed to
 * [filter]/[matches]. There is deliberately no fuzzy matching, stemming,
 * tokenization, or regex support.
 */
object ArticleQueryMatcher {
    /** Returns `true` when [article] matches [query] with the rules above. */
    fun matches(
        article: Article,
        query: String,
    ): Boolean {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return false
        val lowerQuery = trimmed.lowercase()
        val titleMatches =
            article.title
                .orEmpty()
                .lowercase()
                .contains(lowerQuery)
        val descriptionMatches =
            article.description
                .orEmpty()
                .lowercase()
                .contains(lowerQuery)
        val sourceMatches =
            article.source
                ?.name
                .orEmpty()
                .lowercase()
                .contains(lowerQuery)
        return titleMatches || descriptionMatches || sourceMatches
    }

    /** Returns the subset of [articles] that match [query], preserving input order. */
    fun filter(
        articles: List<Article>,
        query: String,
    ): List<Article> = articles.filter { matches(it, query) }
}
