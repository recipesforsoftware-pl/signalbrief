package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

/** Browser-session bookmarks intentionally reset when the Web app reloads. */
class WebSavedArticlesRepository : SavedArticlesRepository {
    private val saved = MutableStateFlow<List<Article>>(emptyList())

    override fun observeAllSavedArticles(): Flow<List<Article>> = saved

    override fun isArticleSaved(url: String): Flow<Boolean> =
        saved.map { articles ->
            articles.any { article -> article.url == url }
        }

    override suspend fun saveArticle(article: Article): Result<Unit> =
        runCatching {
            saved.value =
                listOf(article) +
                saved.value.filterNot { savedArticle ->
                    savedArticle.url == article.url
                }
        }

    override suspend fun removeSavedArticle(url: String): Result<Unit> =
        runCatching {
            saved.value =
                saved.value.filterNot { article ->
                    article.url == url
                }
        }
}
