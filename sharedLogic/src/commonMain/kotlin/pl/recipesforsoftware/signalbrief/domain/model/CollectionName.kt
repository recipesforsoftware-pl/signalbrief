package pl.recipesforsoftware.signalbrief.domain.model

/**
 * A validated, normalized collection name.
 *
 * The single deterministic rule for collection names: leading and trailing
 * whitespace is trimmed and a name that is blank-only after trimming is not a
 * valid collection name. No other constraints are imposed by the domain.
 *
 * [value] always holds the trimmed, non-blank normalized name of a valid
 * [CollectionName]; it is only constructed through [from].
 */
class CollectionName private constructor(
    val value: String,
) {
    /**
     * Returns a [CollectionName] for [raw] after trimming leading and trailing
     * whitespace, or `null` when the trimmed result is blank.
     */
    companion object {
        fun from(raw: String): CollectionName? {
            val normalized = raw.trim()
            return if (normalized.isEmpty()) {
                null
            } else {
                CollectionName(normalized)
            }
        }
    }
}
