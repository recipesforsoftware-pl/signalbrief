package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_create
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete_message
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_delete_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_empty_title
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_duplicate_query
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_error_not_found
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_match_summary
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_no_downloaded_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_options
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.topic_monitoring_rename
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicEditor
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiError
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiState

private fun resourceString(
    resource: StringResource,
    vararg args: Any,
): String = runBlocking { getString(resource, *args) }

private fun pluralString(
    resource: PluralStringResource,
    quantity: Int,
): String = runBlocking { getPluralString(resource, quantity, quantity) }

class TopicMonitoringScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun content(
        state: TopicMonitoringUiState,
        create: () -> Unit = {},
        rename: (MonitoredTopic) -> Unit = {},
        delete: (MonitoredTopic) -> Unit = {},
        open: (MonitoredTopic) -> Unit = {},
    ) = rule.setContent {
        SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
            TopicMonitoringScreen(state, create, rename, {}, {}, {}, delete, {}, {}, {}, open, {})
        }
    }

    @Test
    fun emptyStateAndCreateAffordanceRender() {
        var opened = false
        content(TopicMonitoringUiState(), create = { opened = true })
        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_empty_title)).assertIsDisplayed()
        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_create)).performClick()
        check(opened)
    }

    @Test
    fun rowOverflowTargetsSelectedTopicOnly() {
        val first = MonitoredTopic("1", "Kotlin")
        val second = MonitoredTopic("2", "Compose")
        var renamed: MonitoredTopic? = null
        var deleted: MonitoredTopic? = null
        content(
            TopicMonitoringUiState(topics = listOf(first, second)),
            { },
            { renamed = it },
            { deleted = it },
        )
        rule.onAllNodesWithContentDescription(resourceString(Res.string.topic_monitoring_options))[1].performClick()
        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_rename)).performClick()
        check(renamed == second)
        check(deleted == null)
        rule.onAllNodesWithContentDescription(resourceString(Res.string.topic_monitoring_options))[0].performClick()
        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_delete)).performClick()
        check(deleted == first)
    }

    @Test
    fun topicRowPrimaryActionOpensMatchesForSelectedTopic() {
        val first = MonitoredTopic("1", "Kotlin")
        val second = MonitoredTopic("2", "Compose")
        var opened: MonitoredTopic? = null
        content(
            TopicMonitoringUiState(topics = listOf(first, second)),
            { },
            { },
            { },
            { opened = it },
        )

        rule.onNodeWithText(second.query).performClick()
        check(opened == second)
    }

    @Test
    fun overflowIconDoesNotOpenTopicMatches() {
        val topic = MonitoredTopic("1", "Kotlin")
        var opened: MonitoredTopic? = null
        content(
            TopicMonitoringUiState(topics = listOf(topic)),
            { },
            { },
            { },
            { opened = it },
        )

        rule.onNodeWithContentDescription(resourceString(Res.string.topic_monitoring_options)).performClick()
        check(opened == null)
    }

    @Test
    fun singleMatchSummaryIsSingular() {
        content(
            TopicMonitoringUiState(
                topics = listOf(MonitoredTopic("1", "Kotlin")),
                matchCountsByTopicId = mapOf("1" to 1),
                hasLocalArticles = true,
            ),
        )

        rule.onNodeWithText(pluralString(Res.plurals.topic_monitoring_match_summary, 1)).assertIsDisplayed()
    }

    @Test
    fun pluralMatchSummaryIsPlural() {
        content(
            TopicMonitoringUiState(
                topics = listOf(MonitoredTopic("1", "Kotlin")),
                matchCountsByTopicId = mapOf("1" to 3),
                hasLocalArticles = true,
            ),
        )

        rule.onNodeWithText(pluralString(Res.plurals.topic_monitoring_match_summary, 3)).assertIsDisplayed()
    }

    @Test
    fun missingLocalCacheShowsNoDownloadedHeadlines() {
        content(
            TopicMonitoringUiState(
                topics = listOf(MonitoredTopic("1", "Kotlin")),
            ),
        )

        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_no_downloaded_headlines)).assertIsDisplayed()
    }

    @Test
    fun matchSummaryDoesNotDisplacePrimaryAction() {
        val first = MonitoredTopic("1", "Kotlin")
        val second = MonitoredTopic("2", "Compose")
        var opened: MonitoredTopic? = null
        content(
            TopicMonitoringUiState(
                topics = listOf(first, second),
                matchCountsByTopicId = mapOf("1" to 1, "2" to 3),
                hasLocalArticles = true,
            ),
            { },
            { },
            { },
            { opened = it },
        )

        rule.onNodeWithText(second.query).performClick()
        check(opened == second)
    }

    @Test
    fun overflowStillDoesNotOpenTopicMatchesWithSummaries() {
        val topic = MonitoredTopic("1", "Kotlin")
        var opened: MonitoredTopic? = null
        content(
            TopicMonitoringUiState(
                topics = listOf(topic),
                matchCountsByTopicId = mapOf("1" to 2),
                hasLocalArticles = true,
            ),
            { },
            { },
            { },
            { opened = it },
        )

        rule.onNodeWithContentDescription(resourceString(Res.string.topic_monitoring_options)).performClick()
        check(opened == null)
    }

    @Test
    fun editorErrorIsVisible() {
        content(
            TopicMonitoringUiState(
                editor = TopicEditor.Create(),
                error = TopicMonitoringUiError.DuplicateQuery,
            ),
        )

        rule
            .onNodeWithText(resourceString(Res.string.topic_monitoring_error_duplicate_query))
            .assertIsDisplayed()
    }

    @Test
    fun selectedDeleteTopicIsVisible() {
        val topic = MonitoredTopic("2", "Compose")

        content(
            TopicMonitoringUiState(
                pendingDelete = topic,
            ),
        )

        rule.onNodeWithText(resourceString(Res.string.topic_monitoring_delete_title)).assertIsDisplayed()
        rule
            .onNodeWithText(resourceString(Res.string.topic_monitoring_delete_message, topic.query))
            .assertIsDisplayed()
    }

    @Test
    fun nonEditorErrorIsVisibleInSnackbar() {
        content(
            TopicMonitoringUiState(
                error = TopicMonitoringUiError.NotFound,
            ),
        )

        rule
            .onNodeWithText(resourceString(Res.string.topic_monitoring_error_not_found))
            .assertIsDisplayed()
    }
}
