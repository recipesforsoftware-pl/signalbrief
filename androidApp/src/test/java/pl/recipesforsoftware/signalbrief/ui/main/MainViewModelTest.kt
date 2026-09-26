package pl.recipesforsoftware.signalbrief.ui.main

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun `cold counter starts at UI zero updates and cancels immediately on each collection`() =
        runTest {
            val updates = MutableSharedFlow<List<Article>>()
            var collectors = 0
            val repository = mockk<SavedArticlesRepository>()
            every { repository.observeAllSavedArticles() } returns
                flow {
                    collectors++
                    try {
                        updates.collect { emit(it) }
                    } finally {
                        collectors--
                    }
                }
            val viewModel = MainViewModel(repository)
            runCurrent()
            assertEquals(0, collectors)
            repeat(2) {
                var uiCount = 0
                val job = launch { viewModel.savedArticleCount.collect { uiCount = it } }
                assertEquals(0, uiCount)
                runCurrent()
                assertEquals(1, collectors)
                updates.emit(List(3) { Article(null, null, "https://example.invalid/$it", null, null) })
                runCurrent()
                assertEquals(3, uiCount)
                updates.emit(emptyList())
                runCurrent()
                assertEquals(0, uiCount)
                job.cancel()
                runCurrent()
                assertEquals(0, collectors)
            }
        }
}
