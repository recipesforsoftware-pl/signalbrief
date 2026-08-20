package pl.recipesforsoftware.signalbrief.ui.saved

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
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeSavedArticlesRepositoryForSavedTests : SavedArticlesRepository {
    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    val savedArticles: StateFlow<List<Article>> = _savedArticles

    var removeCalls = mutableListOf<String>()

    override fun observeAllSavedArticles() = _savedArticles

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(_savedArticles.value.any { it.url == url })

    override suspend fun saveArticle(article: Article): Result<Unit> {
        _savedArticles.value = _savedArticles.value + article
        return Result.success(Unit)
    }

    override suspend fun removeSavedArticle(url: String): Result<Unit> {
        removeCalls.add(url)
        _savedArticles.value = _savedArticles.value.filter { it.url != url }
        return Result.success(Unit)
    }
}

private class FailingSavedArticlesRepository : SavedArticlesRepository {
    override fun observeAllSavedArticles(): Flow<List<Article>> = flowOf(emptyList())

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
private fun createPresenter(
    savedArticlesRepository: SavedArticlesRepository,
    scope: TestScope,
): SavedArticlesPresenter =
    SavedArticlesPresenter(
        savedArticlesRepository = savedArticlesRepository,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class SavedArticlesPresenterTest {
    @Test
    fun `empty repository maps to empty state`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            assertEquals(SavedArticlesUiState.Empty, presenter.uiState.value)
        }

    @Test
    fun `persisted articles map to content in repository order`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            savedRepo.saveArticle(testArticle(1))
            savedRepo.saveArticle(testArticle(2))

            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            val state = assertIs<SavedArticlesUiState.Content>(presenter.uiState.value)
            assertEquals(2, state.articles.size)
            assertEquals("https://example.com/1", state.articles[0].url)
            assertEquals("https://example.com/2", state.articles[1].url)
        }

    @Test
    fun `reactive repository emission updates state`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            assertEquals(SavedArticlesUiState.Empty, presenter.uiState.value)

            savedRepo.saveArticle(testArticle(1))
            advanceUntilIdle()

            val state = assertIs<SavedArticlesUiState.Content>(presenter.uiState.value)
            assertEquals(1, state.articles.size)
        }

    @Test
    fun `remove delegates to removeSavedArticle`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            savedRepo.saveArticle(testArticle(1))

            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            presenter.removeArticle("https://example.com/1")
            advanceUntilIdle()

            assertEquals(1, savedRepo.removeCalls.size)
            assertEquals("https://example.com/1", savedRepo.removeCalls.first())
        }

    @Test
    fun `successful persistence emission removes item`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            savedRepo.saveArticle(testArticle(1))
            savedRepo.saveArticle(testArticle(2))

            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            val initial = assertIs<SavedArticlesUiState.Content>(presenter.uiState.value)
            assertEquals(2, initial.articles.size)

            presenter.removeArticle("https://example.com/1")
            advanceUntilIdle()

            val updated = assertIs<SavedArticlesUiState.Content>(presenter.uiState.value)
            assertEquals(1, updated.articles.size)
            assertEquals("https://example.com/2", updated.articles[0].url)
        }

    @Test
    fun `remove failure does not falsely remove item`() =
        runTest {
            val failingRepo = FailingSavedArticlesRepository()
            val presenter = createPresenter(failingRepo, this)
            advanceUntilIdle()

            val state = assertIs<SavedArticlesUiState.Empty>(presenter.uiState.value)
            assertEquals(SavedArticlesUiState.Empty, state)
        }

    @Test
    fun `dispose cancels the collection`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            presenter.dispose()
            advanceUntilIdle()

            savedRepo.saveArticle(testArticle(1))
            advanceUntilIdle()

            assertEquals(SavedArticlesUiState.Empty, presenter.uiState.value)
        }

    @Test
    fun `articles appear in repository emission order`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForSavedTests()
            val presenter = createPresenter(savedRepo, this)
            advanceUntilIdle()

            savedRepo.saveArticle(testArticle(1))
            advanceUntilIdle()

            savedRepo.saveArticle(testArticle(2))
            advanceUntilIdle()

            val state = assertIs<SavedArticlesUiState.Content>(presenter.uiState.value)
            assertEquals("https://example.com/1", state.articles[0].url)
            assertEquals("https://example.com/2", state.articles[1].url)
        }
}
