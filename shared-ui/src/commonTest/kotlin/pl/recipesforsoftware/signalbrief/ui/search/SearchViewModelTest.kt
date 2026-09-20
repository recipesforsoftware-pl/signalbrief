package pl.recipesforsoftware.signalbrief.ui.search

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

private class FakeNewsRepositoryForSearch : NewsRepository {
    private val cachedArticles = MutableStateFlow<List<Article>>(emptyList())

    var getTopHeadlinesCallCount: Int = 0

    fun seed(articles: List<Article>) {
        cachedArticles.value = articles
    }

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        getTopHeadlinesCallCount++
        error("Search must never call the network-first feed path")
    }

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cachedArticles

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> = Result.success(Unit)
}

private class FakeSavedArticlesRepositoryForSearch : SavedArticlesRepository {
    private val savedArticles = MutableStateFlow<List<Article>>(emptyList())

    var lastSavedArticle: Article? = null
    var lastRemovedUrl: String? = null

    override fun observeAllSavedArticles() = savedArticles

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(savedArticles.value.any { it.url == url })

    override suspend fun saveArticle(article: Article): Result<Unit> {
        lastSavedArticle = article
        savedArticles.value = savedArticles.value + article
        return Result.success(Unit)
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        lastRemovedUrl = url
        savedArticles.value = savedArticles.value.filter { it.url != url }
        return Result.success(Unit)
    }
}

private class FailingSavedArticlesRepository : SavedArticlesRepository {
    override fun observeAllSavedArticles(): Flow<List<Article>> = flowOf(emptyList())

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(false)

    override suspend fun saveArticle(article: Article): Result<Unit> = failure()

    override suspend fun removeSavedArticle(url: String): Result<Unit> = failure()

    private fun failure(): Result<Unit> = Result.failure(IllegalStateException("db error"))
}

private val testSource = Source(id = "test", name = "Test News")

private fun article(
    url: String,
    title: String = "Headline for $url",
    description: String = "Description for $url",
    source: Source = testSource,
): Article =
    Article(
        title = title,
        description = description,
        url = url,
        imageUrl = null,
        source = source,
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun createViewModel(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    initialQuery: String = "",
): SearchViewModel =
    SearchViewModel(
        newsRepository = newsRepository,
        savedArticlesRepository = savedArticlesRepository,
        initialQuery = initialQuery,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun `blank query with cached articles shows idle`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()

            assertEquals(SearchUiState.Idle, viewModel.uiState.value)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `no local articles shows no local articles state`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()

            assertEquals(SearchUiState.NoLocalArticles, viewModel.uiState.value)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `title match returns results`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "Kotlin Multiplatform rocks")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("Kotlin")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(listOf(article), state.articles)
            assertEquals("Kotlin", state.query)
        }

    @Test
    fun `description match returns results`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", description = "A detailed Kotlin story")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("detailed")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(listOf(article), state.articles)
        }

    @Test
    fun `source name match returns results`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val source = Source(id = "src", name = "Kotlin Weekly")
            val article = article("https://example.com/1", source = source)
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("weekly")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(listOf(article), state.articles)
        }

    @Test
    fun `match is case insensitive`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "KOTLIN")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("kotlin")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(listOf(article), state.articles)
        }

    @Test
    fun `query is trimmed before matching`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "Kotlin")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("  kotlin  ")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals("kotlin", state.query)
            assertEquals(listOf(article), state.articles)
        }

    @Test
    fun `blank trimmed query falls back to idle`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("   ")
            advanceUntilIdle()

            assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `no matches shows no results`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            newsRepo.seed(listOf(article("https://example.com/1", title = "Kotlin")))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("swift")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.NoResults>(viewModel.uiState.value)
            assertEquals("swift", state.query)
        }

    @Test
    fun `results preserve source ordering`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val first = article("https://example.com/1", title = "Kotlin first")
            val second = article("https://example.com/2", title = "Kotlin second")
            val third = article("https://example.com/3", title = "Other")
            newsRepo.seed(listOf(first, second, third))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.setQuery("Kotlin")
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(listOf(first, second), state.articles)
        }

    @Test
    fun `local article emissions update results`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val first = article("https://example.com/1", title = "Kotlin old")
            newsRepo.seed(listOf(first))

            val viewModel = createViewModel(newsRepo, savedRepo, initialQuery = "Kotlin")
            advanceUntilIdle()

            val second = article("https://example.com/2", title = "Kotlin new")
            newsRepo.seed(listOf(first, second))
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(2, state.articles.size)
        }

    @Test
    fun `saved urls update reactively`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "Kotlin")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo, initialQuery = "Kotlin")
            advanceUntilIdle()

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(setOf("https://example.com/1"), state.savedUrls)
        }

    @Test
    fun `bookmark save delegates to repository`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "Kotlin")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals(article, savedRepo.lastSavedArticle)
        }

    @Test
    fun `bookmark remove delegates to repository`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            val article = article("https://example.com/1", title = "Kotlin")
            newsRepo.seed(listOf(article))
            savedRepo.saveArticle(article)

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals("https://example.com/1", savedRepo.lastRemovedUrl)
        }

    @Test
    fun `persistence failure does not falsely flip saved state`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FailingSavedArticlesRepository()
            val article = article("https://example.com/1", title = "Kotlin")
            newsRepo.seed(listOf(article))

            val viewModel = createViewModel(newsRepo, savedRepo, initialQuery = "Kotlin")
            advanceUntilIdle()
            viewModel.toggleBookmark(article)
            advanceUntilIdle()

            val state = assertIs<SearchUiState.Results>(viewModel.uiState.value)
            assertEquals(emptySet<String>(), state.savedUrls)
        }

    @Test
    fun `dispose cancels collection`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val viewModel = createViewModel(newsRepo, savedRepo)
            advanceUntilIdle()

            ViewModelStore().apply {
                put("viewModel", viewModel)
                clear()
            }
            advanceUntilIdle()

            newsRepo.seed(listOf(article("https://example.com/2")))
            advanceUntilIdle()

            assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        }

    @Test
    fun `initial query is reflected in query state`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForSearch()
            val savedRepo = FakeSavedArticlesRepositoryForSearch()
            newsRepo.seed(listOf(article("https://example.com/1", title = "Kotlin")))

            val viewModel = createViewModel(newsRepo, savedRepo, initialQuery = "Kotlin")
            advanceUntilIdle()

            assertEquals("Kotlin", viewModel.query.value)
            assertIs<SearchUiState.Results>(viewModel.uiState.value)
        }
}
