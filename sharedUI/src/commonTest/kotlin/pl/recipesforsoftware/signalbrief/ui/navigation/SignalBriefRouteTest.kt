package pl.recipesforsoftware.signalbrief.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class SignalBriefRouteTest {
    @Test
    fun topLevelRoutesRemainExactlyTheBottomBarDestinations() {
        assertEquals(
            setOf(
                SignalBriefRoute.Headlines,
                SignalBriefRoute.DailyBrief,
                SignalBriefRoute.Saved,
            ),
            signalBriefTopLevelRoutes,
        )
        assertEquals(SignalBriefRoute.Headlines, signalBriefStartRoute)
    }

    @Test
    fun detailRoutesCarryStableIdentifiersOnly() {
        assertEquals(
            "https://example.com/article",
            SignalBriefRoute.ArticleDetails("https://example.com/article").articleUrl,
        )
        assertEquals(
            "collection-id",
            SignalBriefRoute.CollectionDetails("collection-id").collectionId,
        )
        assertEquals(
            "topic-id",
            SignalBriefRoute.TopicMatches("topic-id").topicId,
        )
    }
}
