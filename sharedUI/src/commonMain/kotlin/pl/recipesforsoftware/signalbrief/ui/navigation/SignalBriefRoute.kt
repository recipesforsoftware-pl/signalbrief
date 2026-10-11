package pl.recipesforsoftware.signalbrief.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation 3 destinations owned by the shared application shell.
 *
 * Navigation arguments intentionally contain only stable identifiers. Domain
 * objects are resolved by the destination after navigation, which keeps the
 * back stack serializable across Android, iOS, Desktop, and Web.
 */
@Serializable
internal sealed interface SignalBriefRoute : NavKey {
    @Serializable
    @SerialName("headlines")
    data object Headlines : SignalBriefRoute

    @Serializable
    @SerialName("daily_brief")
    data object DailyBrief : SignalBriefRoute

    @Serializable
    @SerialName("saved")
    data object Saved : SignalBriefRoute

    @Serializable
    @SerialName("search")
    data object Search : SignalBriefRoute

    @Serializable
    @SerialName("settings")
    data object Settings : SignalBriefRoute

    @Serializable
    @SerialName("collections")
    data object Collections : SignalBriefRoute

    @Serializable
    @SerialName("collection_details")
    data class CollectionDetails(
        val collectionId: String,
    ) : SignalBriefRoute

    @Serializable
    @SerialName("topic_monitoring")
    data object TopicMonitoring : SignalBriefRoute

    @Serializable
    @SerialName("topic_matches")
    data class TopicMatches(
        val topicId: String,
    ) : SignalBriefRoute

    @Serializable
    @SerialName("article_details")
    data class ArticleDetails(
        val articleUrl: String,
    ) : SignalBriefRoute
}

internal val signalBriefStartRoute: SignalBriefRoute = SignalBriefRoute.Headlines

internal val signalBriefTopLevelRoutes: Set<SignalBriefRoute> =
    setOf(
        SignalBriefRoute.Headlines,
        SignalBriefRoute.DailyBrief,
        SignalBriefRoute.Saved,
    )
