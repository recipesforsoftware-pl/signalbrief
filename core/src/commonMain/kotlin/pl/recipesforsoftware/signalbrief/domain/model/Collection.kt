package pl.recipesforsoftware.signalbrief.domain.model

/**
 * Framework-independent saved-article collection used by the domain and UI
 * layers.
 *
 * [id] is stable and unique per collection and is assigned by the repository
 * implementation. [name] always holds the normalized, non-blank collection name
 * produced by [CollectionName.from]; collection names are never stored with
 * leading or trailing whitespace.
 */
data class Collection(
    val id: String,
    val name: String,
)
