package pl.recipesforsoftware.signalbrief.ui.saved

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeSavedArticlesRepositoryForSavedTests : SavedArticlesRepository {
    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    val savedArticles: StateFlow<List<Article>> = _savedArticles
    val removeCalls = mutableListOf<String>()
    var removeFailure: Throwable? = null

    override fun observeAllSavedArticles() = _savedArticles

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(_savedArticles.value.any { it.url == url })

    override suspend fun saveArticle(article: Article): Result<Unit> {
        _savedArticles.value = _savedArticles.value + article
        return Result.success(Unit)
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        removeCalls.add(url)
        removeFailure?.let { return Result.failure(it) }
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

private fun createViewModel(repository: SavedArticlesRepository) = SavedArticlesViewModel(repository)

@OptIn(ExperimentalCoroutinesApi::class)
private fun runSavedArticlesViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
class SavedArticlesViewModelTest {
    @Test
    fun `empty repository maps to empty state`() =
        runSavedArticlesViewModelTest {
            val viewModel = createViewModel(FakeSavedArticlesRepositoryForSavedTests())
            advanceUntilIdle()

            assertEquals(SavedArticlesUiState.Empty, viewModel.uiState.value)
        }

    @Test
    fun `persisted articles map to content in repository order`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            repository.saveArticle(testArticle(1))
            repository.saveArticle(testArticle(2))
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            val state = assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value)
            assertEquals(listOf("https://example.com/1", "https://example.com/2"), state.articles.map(Article::url))
        }

    @Test
    fun `reactive repository emission updates state`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            repository.saveArticle(testArticle(1))
            advanceUntilIdle()

            assertEquals(1, assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value).articles.size)
        }

    @Test
    fun `remove delegates to removeSavedArticle`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            val viewModel = createViewModel(repository)

            viewModel.removeArticle("https://example.com/1")
            advanceUntilIdle()

            assertEquals(listOf("https://example.com/1"), repository.removeCalls)
        }

    @Test
    fun `successful repository emission removes item`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            repository.saveArticle(testArticle(1))
            repository.saveArticle(testArticle(2))
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            viewModel.removeArticle("https://example.com/1")
            advanceUntilIdle()

            assertEquals(
                listOf("https://example.com/2"),
                assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value).articles.map(Article::url),
            )
        }

    @Test
    fun `remove failure does not falsely remove item`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            repository.saveArticle(testArticle(1))
            repository.removeFailure = IllegalStateException("db error")
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            viewModel.removeArticle("https://example.com/1")
            advanceUntilIdle()

            assertEquals(
                listOf("https://example.com/1"),
                assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value).articles.map(Article::url),
            )
        }

    @Test
    fun `clearing lifecycle owner stops later observation updates`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            repository.saveArticle(testArticle(1))
            val viewModel = createViewModel(repository)
            val viewModelStore = ViewModelStore()
            viewModelStore.put("saved", viewModel)
            advanceUntilIdle()

            viewModelStore.clear()
            repository.saveArticle(testArticle(2))
            advanceUntilIdle()

            assertEquals(
                listOf("https://example.com/1"),
                assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value).articles.map(Article::url),
            )
        }

    @Test
    fun `articles appear in repository emission order`() =
        runSavedArticlesViewModelTest {
            val repository = FakeSavedArticlesRepositoryForSavedTests()
            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            repository.saveArticle(testArticle(1))
            repository.saveArticle(testArticle(2))
            advanceUntilIdle()

            assertEquals(
                listOf("https://example.com/1", "https://example.com/2"),
                assertIs<SavedArticlesUiState.Content>(viewModel.uiState.value).articles.map(Article::url),
            )
        }
}
