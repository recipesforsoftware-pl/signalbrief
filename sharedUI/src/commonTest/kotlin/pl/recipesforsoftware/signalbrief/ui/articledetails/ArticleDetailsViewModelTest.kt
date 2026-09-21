package pl.recipesforsoftware.signalbrief.ui.articledetails

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private fun runViewModelTest(block: suspend TestScope.() -> Unit) =
    runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

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
private fun createViewModel(
    savedArticlesRepository: SavedArticlesRepository,
    article: Article,
): ArticleDetailsViewModel =
    ArticleDetailsViewModel(
        savedArticlesRepository = savedArticlesRepository,
        article = article,
    )

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleDetailsViewModelTest {
    @Test
    fun `unsaved article starts as unsaved`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            val state = assertIs<ArticleDetailsUiState>(viewModel.uiState.value)
            assertFalse(state.isSaved, "Unsaved article should not be marked saved")
            assertEquals(article, state.article)
        }

    @Test
    fun `persisted article appears as saved`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            val state = assertIs<ArticleDetailsUiState>(viewModel.uiState.value)
            assertTrue(state.isSaved, "Persisted article should be marked saved")
        }

    @Test
    fun `repository emission updates bookmark state reactively`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isSaved)

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSaved, "Save emission should flip details state to saved")

            savedRepo.removeSavedArticle(article.url)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isSaved, "Remove emission should flip details state to unsaved")
        }

    @Test
    fun `toggle on unsaved article delegates to saveArticle`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            viewModel.toggleBookmark()
            advanceUntilIdle()

            assertEquals(1, savedRepo.saveCalls.size)
            assertEquals(article, savedRepo.saveCalls.first())
            assertEquals(0, savedRepo.removeCalls.size)
        }

    @Test
    fun `toggle on saved article delegates to removeSavedArticle`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)
            savedRepo.saveArticle(article)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            viewModel.toggleBookmark()
            advanceUntilIdle()

            assertEquals(1, savedRepo.removeCalls.size)
            assertEquals(article.url, savedRepo.removeCalls.first())
            assertEquals(1, savedRepo.saveCalls.size)
        }

    @Test
    fun `save failure does not falsely mark article saved`() =
        runViewModelTest {
            val failingRepo = FailingSavedArticlesRepositoryForDetails()
            val article = testArticle(1)

            val viewModel = createViewModel(failingRepo, article)
            advanceUntilIdle()

            viewModel.toggleBookmark()
            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.isSaved,
                "Failed save must not leave the details state saved",
            )
        }

    @Test
    fun `remove failure does not falsely mark article unsaved`() =
        runViewModelTest {
            val article = testArticle(1)
            val failingRepo =
                object : SavedArticlesRepository by FailingSavedArticlesRepositoryForDetails() {
                    override fun isArticleSaved(url: String): Flow<Boolean> = flowOf(true)
                }

            val viewModel = createViewModel(failingRepo, article)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSaved)

            viewModel.toggleBookmark()
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.isSaved,
                "Failed remove must keep the persisted saved state",
            )
        }

    @Test
    fun `state converges to persistence truth after initial emission`() =
        runViewModelTest {
            val article = testArticle(1)
            val slowRepo = SlowEmissionSavedArticlesRepository(savedValue = true)

            val viewModel = createViewModel(slowRepo, article)

            assertFalse(
                viewModel.uiState.value.isSaved,
                "Before the first emission the viewModel must not guess the saved state",
            )

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.isSaved,
                "After the persistence emission the state must mirror the repository",
            )
        }

    @Test
    fun `dispose cancels the collection`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article = testArticle(1)

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            ViewModelStore().apply {
                put("viewModel", viewModel)
                clear()
            }
            advanceUntilIdle()

            savedRepo.saveArticle(article)
            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.isSaved,
                "After dispose the viewModel must stop observing persistence changes",
            )
        }

    @Test
    fun `bookmark toggle is blocked for non actionable url`() =
        runViewModelTest {
            val savedRepo = FakeSavedArticlesRepositoryForDetailsTests()
            val article =
                Article(
                    title = "No URL",
                    description = null,
                    url = "   ",
                    imageUrl = null,
                    source = null,
                )

            val viewModel = createViewModel(savedRepo, article)
            advanceUntilIdle()

            viewModel.toggleBookmark()
            advanceUntilIdle()

            assertEquals(0, savedRepo.saveCalls.size, "Blank URL must not be saved")
            assertEquals(0, savedRepo.removeCalls.size)
        }
}
