package pl.recipesforsoftware.signalbrief.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Cross-platform serialization configuration for Navigation 3 back-stack keys.
 *
 * Non-JVM targets cannot rely on reflection, so every [SignalBriefRoute] is
 * registered explicitly under [NavKey]. This configuration will be consumed by
 * rememberNavBackStack when the existing state router is migrated in later PRs.
 */
internal val signalBriefNavigationSavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(
                        SignalBriefRoute.Headlines::class,
                        SignalBriefRoute.Headlines.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.DailyBrief::class,
                        SignalBriefRoute.DailyBrief.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.Saved::class,
                        SignalBriefRoute.Saved.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.Search::class,
                        SignalBriefRoute.Search.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.Settings::class,
                        SignalBriefRoute.Settings.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.Collections::class,
                        SignalBriefRoute.Collections.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.CollectionDetails::class,
                        SignalBriefRoute.CollectionDetails.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.TopicMonitoring::class,
                        SignalBriefRoute.TopicMonitoring.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.TopicMatches::class,
                        SignalBriefRoute.TopicMatches.serializer(),
                    )
                    subclass(
                        SignalBriefRoute.ArticleDetails::class,
                        SignalBriefRoute.ArticleDetails.serializer(),
                    )
                }
            }
    }
