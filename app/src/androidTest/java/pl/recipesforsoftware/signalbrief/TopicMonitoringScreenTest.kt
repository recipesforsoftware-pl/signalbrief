package pl.recipesforsoftware.signalbrief

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.ui.theme.SignalBriefAndroidTheme
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicEditor
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringScreen
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringStrings
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiError
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringUiState

class TopicMonitoringScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun content(
        state: TopicMonitoringUiState,
        create: () -> Unit = {},
        rename: (MonitoredTopic) -> Unit = {},
        delete: (MonitoredTopic) -> Unit = {},
    ) = rule.setContent {
        SignalBriefAndroidTheme(isDarkMode = false, dynamicColor = false) {
            TopicMonitoringScreen(state, create, rename, {}, {}, {}, delete, {}, {}, {}, {})
        }
    }

    @Test
    fun emptyStateAndCreateAffordanceRender() {
        var opened = false
        content(TopicMonitoringUiState(), create = { opened = true })
        rule.onNodeWithText(TopicMonitoringStrings.EMPTY_TITLE).assertIsDisplayed()
        rule.onNodeWithText(TopicMonitoringStrings.CREATE_TOPIC).performClick()
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
        rule.onAllNodesWithContentDescription(TopicMonitoringStrings.OPTIONS)[1].performClick()
        rule.onNodeWithText(TopicMonitoringStrings.RENAME).performClick()
        check(renamed == second)
        check(deleted == null)
        rule.onAllNodesWithContentDescription(TopicMonitoringStrings.OPTIONS)[0].performClick()
        rule.onNodeWithText(TopicMonitoringStrings.DELETE).performClick()
        check(deleted == first)
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
            .onNodeWithText("This topic is already being monitored.")
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

        rule.onNodeWithText(TopicMonitoringStrings.DELETE_TITLE).assertIsDisplayed()
        rule.onNodeWithText("This will permanently remove Compose.").assertIsDisplayed()
    }

    @Test
    fun nonEditorErrorIsVisibleInSnackbar() {
        content(
            TopicMonitoringUiState(
                error = TopicMonitoringUiError.NotFound,
            ),
        )

        rule
            .onNodeWithText("This topic is no longer available.")
            .assertIsDisplayed()
    }
}
