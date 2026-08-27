package pl.recipesforsoftware.signalbrief.web

import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

internal class WebSavedArticlesRepository(
    private val storage: SavedArticlesStorage = BrowserSavedArticlesStorage(),
) : SavedArticlesRepository {
    private val saved = MutableStateFlow(restoreSavedArticles())

    override fun observeAllSavedArticles(): Flow<List<Article>> = saved

    override fun isArticleSaved(url: String): Flow<Boolean> =
        saved.map { articles ->
            articles.any { article -> article.url == url }
        }

    override suspend fun saveArticle(article: Article): Result<Unit> {
        val updated =
            listOf(article) +
                saved.value.filterNot { savedArticle ->
                    savedArticle.url == article.url
                }
        return persist(updated).onSuccess { saved.value = updated }
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        val updated =
            saved.value.filterNot { article ->
                article.url == url
            }
        return persist(updated).onSuccess { saved.value = updated }
    }

    private fun restoreSavedArticles(): List<Article> =
        runCatching {
            storage
                .read(SAVED_ARTICLES_STORAGE_KEY)
                ?.let(SavedArticlesCodec::decode)
                ?.distinctBy(Article::url)
                ?: emptyList()
        }.getOrDefault(emptyList())

    private fun persist(articles: List<Article>): Result<Unit> =
        runCatching {
            storage.write(SAVED_ARTICLES_STORAGE_KEY, SavedArticlesCodec.encode(articles))
        }
}

internal interface SavedArticlesStorage {
    fun read(key: String): String?

    fun write(
        key: String,
        value: String,
    )
}

private class BrowserSavedArticlesStorage : SavedArticlesStorage {
    override fun read(key: String): String? = window.localStorage.getItem(key)

    override fun write(
        key: String,
        value: String,
    ) {
        window.localStorage.setItem(key, value)
    }
}

private object SavedArticlesCodec {
    private val json = Json

    fun encode(articles: List<Article>): String = json.encodeToString(articles.map(Article::toSavedArticle))

    fun decode(payload: String): List<Article> =
        json
            .decodeFromString<List<SavedArticle>>(payload)
            .map(SavedArticle::toArticle)
}

@Serializable
private data class SavedArticle(
    val title: String?,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val source: SavedSource?,
)

@Serializable
private data class SavedSource(
    val id: String?,
    val name: String?,
)

private fun Article.toSavedArticle(): SavedArticle =
    SavedArticle(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = source?.toSavedSource(),
    )

private fun Source.toSavedSource(): SavedSource =
    SavedSource(
        id = id,
        name = name,
    )

private fun SavedArticle.toArticle(): Article =
    Article(
        title = title,
        description = description,
        url = url,
        imageUrl = imageUrl,
        source = source?.toSource(),
    )

private fun SavedSource.toSource(): Source =
    Source(
        id = id,
        name = name,
    )

internal const val SAVED_ARTICLES_STORAGE_KEY = "signalbrief.savedArticles.v1"
