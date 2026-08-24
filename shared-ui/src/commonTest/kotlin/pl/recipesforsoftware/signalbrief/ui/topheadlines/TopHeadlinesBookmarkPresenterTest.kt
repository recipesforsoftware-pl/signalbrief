package pl.recipesforsoftware.signalbrief.ui.topheadlines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeNewsRepositoryForBookmarks : NewsRepository {
    var nextResult: Result<TopHeadlinesFeed> =
        Result.success(TopHeadlinesFeed(emptyList(), FeedSource.NETWORK))

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> = nextResult

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cachedArticles()

    private fun cachedArticles(): Flow<List<Article>> = flowOf(nextResult.getOrNull()?.articles.orEmpty())
}

private class FakeSavedArticlesRepositoryForBookmarks : SavedArticlesRepository {
    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    val savedArticles: StateFlow<List<Article>> = _savedArticles

    var saveCalls = mutableListOf<Article>()
    var removeCalls = mutableListOf<String>()

    override fun observeAllSavedArticles() = _savedArticles

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(_savedArticles.value.any { it.url == url })

    override suspend fun saveArticle(article: Article): Result<Unit> {
        saveCalls.add(article)
        _savedArticles.value = _savedArticles.value + article
        return Result.success(Unit)
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        removeCalls.add(url)
        _savedArticles.value = _savedArticles.value.filter { it.url != url }
        return Result.success(Unit)
    }
}

private val testSource = Source(id = "test", name = "Test News")

private fun testArticle(id: Int): Article =
    Article(
        title = "Headline $id",
        description = "Description $id",
        url = "https://example.com/$id",
        imageUrl = null,
        source = testSource,
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun createPresenterWithBookmarks(
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
    scope: TestScope,
): TopHeadlinesPresenter =
    TopHeadlinesPresenter(
        repository = newsRepository,
        savedArticlesRepository = savedArticlesRepository,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TopHeadlinesBookmarkPresenterTest {
    @Test
    fun `initially unsaved article appears as unsaved in success state`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            newsRepo.nextResult =
                Result.success(
                    TopHeadlinesFeed(listOf(testArticle(1), testArticle(2)), FeedSource.NETWORK),
                )

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertTrue(state.savedUrls.isEmpty(), "No articles should be saved initially")
        }

    @Test
    fun `persisted saved article appears as saved in success state`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            newsRepo.nextResult =
                Result.success(TopHeadlinesFeed(listOf(article, testArticle(2)), FeedSource.NETWORK))

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertTrue(article.url in state.savedUrls, "Saved article should be in savedUrls")
            assertFalse("https://example.com/2" in state.savedUrls)
        }

    @Test
    fun `save toggle delegates to saveArticle`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            val article = testArticle(1)
            newsRepo.nextResult =
                Result.success(TopHeadlinesFeed(listOf(article), FeedSource.NETWORK))

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            presenter.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals(1, savedRepo.saveCalls.size)
            assertEquals(article, savedRepo.saveCalls.first())
        }

    @Test
    fun `remove toggle delegates to removeSavedArticle`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            newsRepo.nextResult =
                Result.success(TopHeadlinesFeed(listOf(article), FeedSource.NETWORK))

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            presenter.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals(1, savedRepo.removeCalls.size)
            assertEquals(article.url, savedRepo.removeCalls.first())
        }

    @Test
    fun `reactive saved state changes update the feed state`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            val article = testArticle(1)
            newsRepo.nextResult =
                Result.success(TopHeadlinesFeed(listOf(article), FeedSource.NETWORK))

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            val initial = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertTrue(initial.savedUrls.isEmpty())

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            val updated = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertTrue(article.url in updated.savedUrls, "Saved URL should appear after persistence")
        }

    @Test
    fun `invalid blank URL cannot trigger save`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val savedRepo = FakeSavedArticlesRepositoryForBookmarks()
            newsRepo.nextResult =
                Result.success(
                    TopHeadlinesFeed(
                        listOf(
                            Article(
                                title = "Blank URL",
                                description = null,
                                url = "   ",
                                imageUrl = null,
                                source = null,
                            ),
                        ),
                        FeedSource.NETWORK,
                    ),
                )

            val presenter = createPresenterWithBookmarks(newsRepo, savedRepo, this)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            val blankArticle = state.articles.first()

            presenter.toggleBookmark(blankArticle)
            advanceUntilIdle()

            assertEquals(0, savedRepo.saveCalls.size, "Blank URL should not be saved")
        }

    @Test
    fun `persistence failure does not falsely mark article saved`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForBookmarks()
            val failingSavedRepo =
                object : SavedArticlesRepository {
                    override fun observeAllSavedArticles() = flowOf(emptyList<Article>())

                    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(false)

                    override suspend fun saveArticle(article: Article): Result<Unit> =
                        Result.failure(
                            IllegalStateException("db error"),
                        )

                    override suspend fun removeSavedArticle(url: String): Result<Unit> =
                        Result.failure(
                            IllegalStateException("db error"),
                        )
                }

            val article = testArticle(1)
            newsRepo.nextResult =
                Result.success(TopHeadlinesFeed(listOf(article), FeedSource.NETWORK))

            val presenter = createPresenterWithBookmarks(newsRepo, failingSavedRepo, this)
            advanceUntilIdle()

            presenter.toggleBookmark(article)
            advanceUntilIdle()

            val state = assertIs<TopHeadlinesUiState.Success>(presenter.uiState.value)
            assertFalse(
                article.url in state.savedUrls,
                "Failed save must not leave article in savedUrls",
            )
        }
}
