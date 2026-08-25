package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

class DemoNewsRepository : NewsRepository {
    private val feed = MutableStateFlow(demoArticles)

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> =
        Result.success(
            TopHeadlinesFeed(feed.value, FeedSource.NETWORK),
        )

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = feed
}

/** Browser-session bookmarks intentionally reset when the demo reloads. */
class WebSavedArticlesRepository : SavedArticlesRepository {
    private val saved = MutableStateFlow<List<Article>>(emptyList())

    override fun observeAllSavedArticles(): Flow<List<Article>> = saved

    override fun isArticleSaved(url: String): Flow<Boolean> = saved.map { list -> list.any { it.url == url } }

    override suspend fun saveArticle(article: Article): Result<Unit> =
        runCatching {
            saved.value = listOf(article) + saved.value.filterNot { it.url == article.url }
        }

    override suspend fun removeSavedArticle(url: String): Result<Unit> =
        runCatching {
            saved.value = saved.value.filterNot { it.url == url }
        }
}

private const val roofGardenThumbnail = "demo-resource://roof-garden"
private const val repairCafeThumbnail = "demo-resource://repair-cafe"

internal val demoArticles =
    listOf(
        demoArticle(
            "City library turns unused roof into a public garden",
            "Civic Dispatch",
            "roof-garden",
            roofGardenThumbnail,
        ),
        demoArticle(
            "A small repair café makes neighborhood skills visible",
            "Local Ledger",
            "repair-cafe",
            repairCafeThumbnail,
        ),
        demoArticle("Night buses add quiet-reading corners on two routes", "Transit Notes", "night-buses", roofGardenThumbnail),
        demoArticle("School makers build an open map of shade trees", "Field Signal", "shade-trees", repairCafeThumbnail),
        demoArticle("Community radio archives a century of local voices", "Archive Weekly", "radio-archive", roofGardenThumbnail),
        demoArticle("Independent bookshops test a shared delivery shelf", "Local Ledger", "bookshops", repairCafeThumbnail),
        demoArticle("River volunteers log the return of spring insects", "Field Signal", "river-insects", roofGardenThumbnail),
        demoArticle("A former warehouse becomes a rehearsal commons", "Civic Dispatch", "rehearsal", repairCafeThumbnail),
        demoArticle("Students prototype signs for safer crossings", "Transit Notes", "crossings", roofGardenThumbnail),
        demoArticle("Public kitchens trade recipes for surplus produce", "Archive Weekly", "kitchens", repairCafeThumbnail),
    )

private fun demoArticle(
    title: String,
    source: String,
    slug: String,
    imageUrl: String,
) = Article(
    title = title,
    description = "A fictional SignalBrief demo story for this browser showcase.",
    url = "https://example.com/signalbrief-demo/$slug",
    imageUrl = imageUrl,
    source = Source(slug, source),
)
