package pl.recipesforsoftware.signalbrief.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pl.recipesforsoftware.signalbrief.domain.failure.NewsFailure
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.FeedSource
import pl.recipesforsoftware.signalbrief.domain.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebNewsRepositoryTest {
    @Test
    fun successfulLoadUpdatesReturnedAndObservedFeed() =
        runTest {
            val expected =
                listOf(
                    Article(
                        title = "Real headline",
                        description = "Description",
                        url = "https://example.com/article",
                        imageUrl = "https://example.com/image.jpg",
                        source = Source(id = null, name = "Example News"),
                    ),
                )

            val repository = WebNewsRepository { expected }

            val result = repository.getTopHeadlines(country = "us")
            val feed = result.getOrThrow()

            assertEquals(expected, feed.articles)
            assertEquals(FeedSource.NETWORK, feed.source)
            assertEquals(
                expected,
                repository.observeCachedTopHeadlines("us").first(),
            )
        }

    @Test
    fun typedNetworkFailureIsPreserved() =
        runTest {
            val repository =
                WebNewsRepository {
                    throw NewsFailure.Network
                }

            val result = repository.getTopHeadlines(country = "us")

            assertTrue(result.isFailure)
            assertIs<NewsFailure.Network>(result.exceptionOrNull())
        }
}
