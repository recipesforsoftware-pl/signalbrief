package pl.recipesforsoftware.signalbrief.ui.articledetails

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeSavedArticlesRepositoryForDetailsTests : SavedArticlesRepository {
    private val _savedArticles = MutableStateFlow<List<Article>>(emptyList())
    val savedArticles: StateFlow<List<Article>> = _savedArticles

    var saveCalls = mutableListOf<Article>()
    var removeCalls = mutableListOf<String>()

    override fun observeAllSavedArticles() = _savedArticles

    override fun isArticleSaved(url: String): Flow<Boolean> =
        _savedArticles.map { articles ->
            articles.any { it.url == url }
        }

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

private class FailingSavedArticlesRepositoryForDetails : SavedArticlesRepository {
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

private class SlowEmissionSavedArticlesRepository(
    private val savedValue: Boolean,
) : SavedArticlesRepository {
    override fun observeAllSavedArticles(): Flow<List<Article>> = flowOf(emptyList())

    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(savedValue)

    override suspend fun saveArticle(article: Article): Result<Unit> = Result.success(Unit)

    override suspend fun removeSavedArticle(url: String): Result<Unit> = Result.success(Unit)
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
    article: Article,
    scope: TestScope,
): ArticleDetailsPresenter =
    ArticleDetailsPresenter(
        savedArticlesRepository = savedArticlesRepository,
        article = article,
        dispatcher = StandardTestDispatcher(scope.testScheduler),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailsPresenterTest {
    @Test
    fun `unsaved article starts as unsaved`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            val state = assertIs<ArticleDetailsUiState>(presenter.uiState.value)
            assertFalse(state.isSaved, "Unsaved article should not be marked saved")
            assertEquals(article, state.article)
        }

    @Test
    fun `persisted article appears as saved`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            val state = assertIs<ArticleDetailsUiState>(presenter.uiState.value)
            assertTrue(state.isSaved, "Persisted article should be marked saved")
        }

    @Test
    fun `repository emission updates bookmark state reactively`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()
            assertFalse(presenter.uiState.value.isSaved)

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isSaved, "Save emission should flip details state to saved")

            savedRepo.removeSavedArticle(article.url)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isSaved, "Remove emission should flip details state to unsaved")
        }

    @Test
    fun `toggle on unsaved article delegates to saveArticle`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            presenter.toggleBookmark()
            advanceUntilIdle()

            assertEquals(1, savedRepo.saveCalls.size)
            assertEquals(article, savedRepo.saveCalls.first())
            assertEquals(0, savedRepo.removeCalls.size)
        }

    @Test
    fun `toggle on saved article delegates to removeSavedArticle`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            presenter.toggleBookmark()
            advanceUntilIdle()

            assertEquals(1, savedRepo.removeCalls.size)
            assertEquals(article.url, savedRepo.removeCalls.first())
            assertEquals(1, savedRepo.saveCalls.size)
        }

    @Test
    fun `save failure does not falsely mark article saved`() =
        runTest {
            val failingRepo = FailingSavedArticlesRepositoryForDetails()
            val article = testArticle(1)

            val presenter = createPresenter(failingRepo, article, this)
            advanceUntilIdle()

            presenter.toggleBookmark()
            advanceUntilIdle()

            assertFalse(
                presenter.uiState.value.isSaved,
                "Failed save must not leave the details state saved",
            )
        }

    @Test
    fun `remove failure does not falsely mark article unsaved`() =
        runTest {
            val article = testArticle(1)
            val failingRepo =
                object : SavedArticlesRepository by FailingSavedArticlesRepositoryForDetails() {
                    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(true)
                }

            val presenter = createPresenter(failingRepo, article, this)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isSaved)

            presenter.toggleBookmark()
            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.isSaved,
                "Failed remove must keep the persisted saved state",
            )
        }

    @Test
    fun `state converges to persistence truth after initial emission`() =
        runTest {
            val article = testArticle(1)
            val slowRepo = SlowEmissionSavedArticlesRepository(savedValue = true)

            val presenter = createPresenter(slowRepo, article, this)

            assertFalse(
                presenter.uiState.value.isSaved,
                "Before the first emission the presenter must not guess the saved state",
            )

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.isSaved,
                "After the persistence emission the state must mirror the repository",
            )
        }

    @Test
    fun `dispose cancels the collection`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            presenter.dispose()
            advanceUntilIdle()

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            assertFalse(
                presenter.uiState.value.isSaved,
                "After dispose the presenter must stop observing persistence changes",
            )
        }

    @Test
    fun `bookmark toggle is blocked for non actionable url`() =
        runTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article =
                Article(
                    title = "No URL",
                    description = null,
                    url = "   ",
                    imageUrl = null,
                    source = null,
                )

            val presenter = createPresenter(savedRepo, article, this)
            advanceUntilIdle()

            presenter.toggleBookmark()
            advanceUntilIdle()

            assertEquals(0, savedRepo.saveCalls.size, "Blank URL must not be saved")
            assertEquals(0, savedRepo.removeCalls.size)
        }
}
