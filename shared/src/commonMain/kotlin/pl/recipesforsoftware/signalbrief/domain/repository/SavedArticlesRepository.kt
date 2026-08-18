package pl.recipesforsoftware.signalbrief.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Domain-facing contract for the user-saved articles persistence.
 *
 * Saved articles are independent of the rotating top-headlines cache and must
 * survive normal cache refresh, replacement, and eviction. Implementations live
 * in the data layer; domain and UI code consume domain models through this
 * boundary only.
 */
interface SavedArticlesRepository {
    /**
     * Returns all saved articles ordered by most recently saved first.
     */
    fun observeAllSavedArticles(): Flow<List<Article>>

    /**
     * Returns whether the article with the given [url] is currently saved.
     */
    fun isArticleSaved(url: String): Flow<Boolean>

    /**
     * Persists a snapshot of [article] as a saved article. If the article is
     * already saved its metadata is refreshed. Failures are returned as a
     * [Result.failure]; coroutine cancellation is always rethrown.
     */
    suspend fun saveArticle(article: Article): Result<Unit>

    /**
     * Removes the saved article with the given [url]. Removing an article that
     * is not saved is a no-op. Failures are returned as a
     * [Result.failure]; coroutine cancellation is always rethrown.
     */
    suspend fun removeSavedArticle(url: String): Result<Unit>
}
