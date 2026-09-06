package pl.recipesforsoftware.signalbrief.domain.failure

import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository

/**
 * Typed failures reported by [TopicMonitoringRepository].
 *
 * Kept small and explicit so that domain and UI code switch on these types
 * instead of leaking database or persistence exceptions.
 */
sealed class TopicMonitoringFailure : Throwable() {
    /** The supplied query is blank after trimming and was rejected. */
    data object InvalidQuery : TopicMonitoringFailure()

    /**
     * The supplied (trimmed, case-insensitive) query already matches an
     * existing monitored topic.
     */
    data object DuplicateQuery : TopicMonitoringFailure()

    /** No monitored topic exists with the supplied id. */
    data object NotFound : TopicMonitoringFailure()
}
