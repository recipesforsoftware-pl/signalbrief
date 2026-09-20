package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesStrings
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesUiState

class TopicMatchesScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val topic = MonitoredTopic(id = "1", query = "Kotlin")
    private val article =
        Article(
            title = "Kotlin Multiplatform reaches 1.0",
            description = "The team ships a stable release.",
            url = "https://example.com/story",
            imageUrl = null,
            source = Source(id = "src", name = "Example News"),
        )

    @Test
    fun noLocalArticlesStateRendersDedicatedCopy() {
        rule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TopicMatchesScreen(
                    uiState = TopicMatchesUiState.NoLocalArticles(topic),
                    onArticleClick = {},
                    onBookmarkClick = {},
                    onBack = {},
                )
            }
        }

        rule.onNodeWithText("Kotlin").assertIsDisplayed()
        rule.onNodeWithText(TopicMatchesStrings.NO_LOCAL_ARTICLES_TITLE).assertIsDisplayed()
        rule.onNodeWithText(TopicMatchesStrings.NO_LOCAL_ARTICLES_SUBTITLE).assertIsDisplayed()
    }

    @Test
    fun noMatchesStateRendersDedicatedCopy() {
        rule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TopicMatchesScreen(
                    uiState = TopicMatchesUiState.NoMatches(topic),
                    onArticleClick = {},
                    onBookmarkClick = {},
                    onBack = {},
                )
            }
        }

        rule.onNodeWithText("Kotlin").assertIsDisplayed()
        rule.onNodeWithText(TopicMatchesStrings.NO_MATCHES_TITLE).assertIsDisplayed()
        rule.onNodeWithText(TopicMatchesStrings.NO_MATCHES_SUBTITLE).assertIsDisplayed()
    }

    @Test
    fun contentRendersSelectedTopicAndExpectedArticle() {
        rule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TopicMatchesScreen(
                    uiState =
                        TopicMatchesUiState.Content(
                            topic = topic,
                            articles = listOf(article),
                            savedUrls = emptySet(),
                        ),
                    onArticleClick = {},
                    onBookmarkClick = {},
                    onBack = {},
                )
            }
        }

        rule.onNodeWithText("Kotlin").assertIsDisplayed()
        rule.onNodeWithText("Kotlin Multiplatform reaches 1.0").assertIsDisplayed()
    }

    @Test
    fun articleClickCallbackTargetsCorrectArticle() {
        val first =
            Article(
                title = "First Kotlin story",
                description = null,
                url = "https://example.com/1",
                imageUrl = null,
                source = Source(null, "Example News"),
            )
        val second =
            Article(
                title = "Second Kotlin story",
                description = null,
                url = "https://example.com/2",
                imageUrl = null,
                source = Source(null, "Example News"),
            )
        var clicked: Article? = null
        rule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TopicMatchesScreen(
                    uiState =
                        TopicMatchesUiState.Content(
                            topic = topic,
                            articles = listOf(first, second),
                            savedUrls = emptySet(),
                        ),
                    onArticleClick = { clicked = it },
                    onBookmarkClick = {},
                    onBack = {},
                )
            }
        }

        rule.onNodeWithText(second.title.orEmpty()).performClick()
        check(clicked == second)
    }

    @Test
    fun bookmarkCallbackTargetsCorrectArticle() {
        var bookmarked: Article? = null
        rule.setContent {
            SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
                TopicMatchesScreen(
                    uiState =
                        TopicMatchesUiState.Content(
                            topic = topic,
                            articles = listOf(article),
                            savedUrls = emptySet(),
                        ),
                    onArticleClick = {},
                    onBookmarkClick = { bookmarked = it },
                    onBack = {},
                )
            }
        }

        rule.onNodeWithContentDescription("Save article").performClick()
        check(bookmarked == article)
    }
}
