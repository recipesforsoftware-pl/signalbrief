package pl.recipesforsoftware.signalbrief.ui.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.model.TopHeadlinesFeed
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeNewsRepositoryForSettings : NewsRepository {
    private val cachedArticles = MutableStateFlow<List<Article>>(emptyList())

    var getTopHeadlinesCallCount: Int = 0
    var clearCallCount: Int = 0
    var clearFailure: Result<Unit>? = null

    fun seed(articles: List<Article>) {
        cachedArticles.value = articles
    }

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        getTopHeadlinesCallCount++
        error("Settings must never call the network-first feed path")
    }

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> {
        clearCallCount++
        clearFailure?.let { return it }
        cachedArticles.value = emptyList()
        return Result.success(Unit)
    }

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cachedArticles
}

private class FailingNewsRepositoryForSettings : NewsRepository {
    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> = error("Not expected")

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> = error("Not expected")

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = failingFlow()

    private fun failingFlow(): Flow<List<Article>> =
        flow {
            throw IllegalStateException("Cache observation failed")
        }
}

private val testSource = Source(id = "test", name = "Test News")

private fun article(
    url: String,
    title: String = "Headline for $url",
): Article =
    Article(
        title = title,
        description = "Description for $url",
        url = url,
        imageUrl = null,
        source = testSource,
    )

@OptIn(ExperimentalCoroutinesApi::class)
private fun createPresenter(
    newsRepository: NewsRepository,
    scope: TestScope,
): SettingsPresenter =
    SettingsPresenter(
        newsRepository = newsRepository,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsPresenterTest {
    @Test
    fun `cached list produces correct count`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(
                listOf(
                    article("https://example.com/1"),
                    article("https://example.com/2"),
                    article("https://example.com/3"),
                ),
            )

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            assertEquals(SettingsUiState(downloadedHeadlineCount = 3), presenter.uiState.value)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `empty cache produces zero count`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            assertEquals(SettingsUiState(downloadedHeadlineCount = 0), presenter.uiState.value)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `later cache emission updates count reactively`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()
            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)

            newsRepo.seed(
                listOf(
                    article("https://example.com/1"),
                    article("https://example.com/2"),
                ),
            )
            advanceUntilIdle()
            assertEquals(SettingsUiState(downloadedHeadlineCount = 2), presenter.uiState.value)
        }

    @Test
    fun `observation failure results in zero state`() =
        runTest {
            val newsRepo = FailingNewsRepositoryForSettings()

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            assertEquals(SettingsUiState(downloadedHeadlineCount = 0), presenter.uiState.value)
        }

    @Test
    fun `getTopHeadlines is never called`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `dispose stops later observation updates`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()
            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)

            presenter.dispose()
            advanceUntilIdle()

            newsRepo.seed(
                listOf(
                    article("https://example.com/1"),
                    article("https://example.com/2"),
                ),
            )
            advanceUntilIdle()

            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)
        }

    @Test
    fun `clear calls the repository exactly once`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            presenter.clearDownloadedHeadlines()
            advanceUntilIdle()

            assertEquals(1, newsRepo.clearCallCount)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `clear never calls getTopHeadlines`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()

            presenter.clearDownloadedHeadlines()
            advanceUntilIdle()

            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
            assertEquals(SettingsUiState(downloadedHeadlineCount = 0), presenter.uiState.value)
        }

    @Test
    fun `count becomes zero only after the cached flow emits an empty list`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()
            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)

            presenter.clearDownloadedHeadlines()
            assertEquals(
                SettingsUiState(downloadedHeadlineCount = 1),
                presenter.uiState.value,
                "Count must not change until the cached flow emits an empty list",
            )

            advanceUntilIdle()

            assertEquals(0, presenter.uiState.value.downloadedHeadlineCount)
        }

    @Test
    fun `failed clear does not fabricate a zero state`() =
        runTest {
            val newsRepo = FakeNewsRepositoryForSettings()
            newsRepo.seed(listOf(article("https://example.com/1")))
            newsRepo.clearFailure = Result.failure(NewsFailure.Unknown(IllegalStateException("database unavailable")))

            val presenter = createPresenter(newsRepo, this)
            advanceUntilIdle()
            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)

            presenter.clearDownloadedHeadlines()
            advanceUntilIdle()

            assertEquals(SettingsUiState(downloadedHeadlineCount = 1), presenter.uiState.value)
            assertEquals(1, newsRepo.clearCallCount)
        }
}
