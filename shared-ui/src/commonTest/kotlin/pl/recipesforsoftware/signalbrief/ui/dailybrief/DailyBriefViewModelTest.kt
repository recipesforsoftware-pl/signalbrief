package pl.recipesforsoftware.signalbrief.ui.dailybrief

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

private class BriefNewsRepository : NewsRepository {
    val cached = MutableStateFlow<List<Article>>(emptyList())
    var remoteCalls = 0

    override suspend fun getTopHeadlines(country: String): Result<TopHeadlinesFeed> {
        remoteCalls++
        error("Daily Brief is local-only")
    }

    override fun observeCachedTopHeadlines(country: String): Flow<List<Article>> = cached

    override suspend fun clearCachedTopHeadlines(country: String): Result<Unit> = Result.success(Unit)
}

private class BriefSavedRepository : SavedArticlesRepository {
    val saved = MutableStateFlow<List<Article>>(emptyList())
    var saveCalls = 0
    var removeCalls = 0
    var saveResult: Result<Unit> = Result.success(Unit)
    var removeResult: Result<Unit> = Result.success(Unit)

    override fun observeAllSavedArticles(): Flow<List<Article>> = saved

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(url in saved.value.map { it.url })

    override suspend fun saveArticle(article: Article): Result<Unit> {
        saveCalls++
        if (saveResult.isSuccess) saved.value += article
        return saveResult
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        removeCalls++
        if (removeResult.isSuccess) {
            saved.value = saved.value.filterNot { it.url == url }
        }
        return removeResult
    }
}

private fun briefArticle(url: String) = Article("Title $url", "Description", url, null, Source("source", "Source"))

@OptIn(ExperimentalCoroutinesApi::class)
private fun viewModel(
    news: NewsRepository,
    saved: SavedArticlesRepository,
) = DailyBriefViewModel(news, saved)

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBriefViewModelTest {
    @Test fun `empty cache maps to empty`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()
            assertEquals(DailyBriefUiState.Empty, viewModel.uiState.value)
            assertEquals(0, news.remoteCalls)
        }

    @Test fun `cached headlines are selected in capped source order`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            news.cached.value = (1..6).map { briefArticle("https://example.com/$it") }
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()
            val state = assertIs<DailyBriefUiState.Content>(viewModel.uiState.value)
            assertEquals((1..5).map { "https://example.com/$it" }, state.articles.map { it.url })
        }

    @Test fun `cache and saved emissions update state reactively`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val article = briefArticle("https://example.com/1")
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()
            news.cached.value = listOf(article)
            advanceUntilIdle()
            saved.saved.value = listOf(article)
            advanceUntilIdle()
            assertEquals(setOf(article.url), assertIs<DailyBriefUiState.Content>(viewModel.uiState.value).savedUrls)
        }

    @Test fun `bookmark save and remove delegate through persistence`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val article = briefArticle("https://example.com/1")
            news.cached.value = listOf(article)
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()
            viewModel.toggleBookmark(article)
            advanceUntilIdle()
            assertEquals(1, saved.saveCalls)
            viewModel.toggleBookmark(article)
            advanceUntilIdle()
            assertEquals(1, saved.removeCalls)
        }

    @Test fun `failed save leaves article unsaved`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val article = briefArticle("https://example.com/1")
            news.cached.value = listOf(article)
            saved.saveResult = Result.failure(IllegalStateException("save failed"))
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()

            viewModel.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals(emptySet(), assertIs<DailyBriefUiState.Content>(viewModel.uiState.value).savedUrls)
        }

    @Test fun `failed remove leaves article saved`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val article = briefArticle("https://example.com/1")
            news.cached.value = listOf(article)
            saved.saved.value = listOf(article)
            saved.removeResult = Result.failure(IllegalStateException("remove failed"))
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()

            viewModel.toggleBookmark(article)
            advanceUntilIdle()

            assertEquals(setOf(article.url), assertIs<DailyBriefUiState.Content>(viewModel.uiState.value).savedUrls)
        }

    @Test fun `dispose stops collecting cache`() =
        runViewModelTest {
            val news = BriefNewsRepository()
            val saved = BriefSavedRepository()
            val viewModel = viewModel(news, saved)
            advanceUntilIdle()
            ViewModelStore().apply {
                put("viewModel", viewModel)
                clear()
            }
            news.cached.value = listOf(briefArticle("https://example.com/1"))
            advanceUntilIdle()
            assertEquals(DailyBriefUiState.Empty, viewModel.uiState.value)
        }
}
