package pl.recipesforsoftware.signalbrief.domain.model

/**
 * Framework-independent monitored topic used by the domain and UI layers.
 *
 * A topic represents a single persisted query the user wants to monitor. [id]
 * is a stable, opaque string assigned by the repository implementation. [query]
 * always holds the normalized, non-blank query produced by [TopicQuery.from];
 * queries are never stored with leading or trailing whitespace.
 */
data class MonitoredTopic(
    val id: String,
    val query: String,
)
