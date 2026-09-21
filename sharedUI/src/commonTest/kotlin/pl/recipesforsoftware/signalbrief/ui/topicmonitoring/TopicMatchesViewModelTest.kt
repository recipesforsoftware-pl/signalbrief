package pl.recipesforsoftware.signalbrief.ui.topicmonitoring

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
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
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

private class FakeNewsRepositoryForMatches : NewsRepository {
    private val cachedArticles = MutableStateFlow<List<Article>>(emptyList())

    var getTopHeadlinesCallCount: Int = 0

    fun seed(articles: List<Article>) {
        cachedArticles.value = articles
    }

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        getTopHeadlinesCallCount++
        error("Topic Matches must never call the network-first feed path")
    }

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cachedArticles

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> = Result.success(Unit)
}

private class FakeSavedArticlesRepositoryForMatches : SavedArticlesRepository {
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
    topic: MonitoredTopic,
    newsRepository: NewsRepository,
    savedArticlesRepository: SavedArticlesRepository,
): TopicMatchesViewModel =
    TopicMatchesViewModel(
        topic = topic,
        newsRepository = newsRepository,
        savedArticlesRepository = savedArticlesRepository,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TopicMatchesViewModelTest {
    @Test
    fun `initial cached emission returns expected matches`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val matching = article("https://example.com/1", title = "Kotlin Multiplatform")
            val other = article("https://example.com/2", title = "Swift news")
            newsRepo.seed(listOf(matching, other))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)
            assertEquals(topic, state.topic)
            assertEquals(listOf(matching), state.articles)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `no local articles shows no local articles state`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.NoLocalArticles>(viewModel.uiState.value)
            assertEquals(topic, state.topic)
            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `local articles with zero matches shows no matches state`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            newsRepo.seed(listOf(article("https://example.com/1", title = "Swift news")))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.NoMatches>(viewModel.uiState.value)
            assertEquals(topic, state.topic)
        }

    @Test
    fun `matching is case insensitive`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "kotlin")
            val matching = article("https://example.com/1", title = "KOTLIN Multiplatform")

            newsRepo.seed(listOf(matching))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)
            assertEquals(listOf(matching), state.articles)
        }

    @Test
    fun `cached feed updates recompute matches reactively`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val first = article("https://example.com/1", title = "Kotlin old")
            newsRepo.seed(listOf(first))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()
            assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)

            val second = article("https://example.com/2", title = "Kotlin new")
            newsRepo.seed(listOf(first, second))
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)
            assertEquals(2, state.articles.size)
        }

    @Test
    fun `result order follows cached feed order`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val first = article("https://example.com/1", title = "Kotlin first")
            val second = article("https://example.com/2", title = "Other")
            val third = article("https://example.com/3", title = "Kotlin second")
            newsRepo.seed(listOf(first, second, third))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)
            assertEquals(listOf(first, third), state.articles)
        }

    @Test
    fun `saved urls are reflected in content`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val matching = article("https://example.com/1", title = "Kotlin Multiplatform")
            newsRepo.seed(listOf(matching))
            savedRepo.saveArticle(matching)

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            val state = assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value)
            assertEquals(setOf("https://example.com/1"), state.savedUrls)
        }

    @Test
    fun `bookmark save delegates to repository`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val matching = article("https://example.com/1", title = "Kotlin Multiplatform")
            newsRepo.seed(listOf(matching))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.toggleBookmark(matching)
            advanceUntilIdle()

            assertEquals(matching, savedRepo.lastSavedArticle)
        }

    @Test
    fun `bookmark remove delegates to repository`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            val matching = article("https://example.com/1", title = "Kotlin Multiplatform")
            newsRepo.seed(listOf(matching))
            savedRepo.saveArticle(matching)

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()
            viewModel.toggleBookmark(matching)
            advanceUntilIdle()

            assertEquals("https://example.com/1", savedRepo.lastRemovedUrl)
        }

    @Test
    fun `topic matching never calls getTopHeadlines`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            newsRepo.seed(listOf(article("https://example.com/1", title = "Kotlin")))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            assertEquals(0, newsRepo.getTopHeadlinesCallCount)
        }

    @Test
    fun `dispose cancels collection`() =
        runViewModelTest {
            val newsRepo = FakeNewsRepositoryForMatches()
            val savedRepo = FakeSavedArticlesRepositoryForMatches()
            val topic = MonitoredTopic("1", "Kotlin")
            newsRepo.seed(listOf(article("https://example.com/1", title = "Kotlin")))

            val viewModel = createViewModel(topic, newsRepo, savedRepo)
            advanceUntilIdle()

            ViewModelStore().apply {
                put("viewModel", viewModel)
                clear()
            }
            advanceUntilIdle()

            newsRepo.seed(listOf(article("https://example.com/2", title = "Kotlin two")))
            advanceUntilIdle()

            assertEquals(
                1,
                assertIs<TopicMatchesUiState.Content>(viewModel.uiState.value).articles.size,
            )
        }
}
