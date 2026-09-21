package pl.recipesforsoftware.signalbrief.domain.model

/**
 * A validated, normalized monitored-topic query.
 *
 * The deterministic rules for a topic query: leading and trailing whitespace is
 * trimmed and a query that is blank-only after trimming is not a valid query.
 * No stemming, tokenization, fuzzy matching, aliases, or search-syntax parsing
 * is applied.
 *
 * [value] always holds the trimmed, non-blank user-facing query of a valid
 * [TopicQuery]; it preserves the user's original casing. [normalizedKey] is a
 * case-insensitive key used for deterministic duplicate detection and ordering:
 * it is the lowercase form of [value]. Two queries that trim to the same text
 * and differ only in casing normalize to the same key and are treated as the
 * same monitored topic.
 */
class TopicQuery private constructor(
    val value: String,
    val normalizedKey: String,
) {
    /**
     * Returns a [TopicQuery] for [raw] after trimming leading and trailing
     * whitespace, or `null` when the trimmed result is blank.
     */
    companion object {
        fun from(raw: String): TopicQuery? {
            val normalized = raw.trim()
            if (normalized.isEmpty()) {
                return null
            }
            return TopicQuery(
                value = normalized,
                normalizedKey = normalized.lowercase(),
            )
        }
    }
}
