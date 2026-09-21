package pl.recipesforsoftware.signalbrief.domain.model

/**
 * Typed top-headlines result that pairs the articles with where they came from.
 *
 * The typed [source] lets the UI distinguish fresh network content from a
 * persistent-cache fallback so it can render the appropriate state (for
 * example a saved-headlines banner) without reaching into the data layer.
 */
data class TopHeadlinesFeed(
    val articles: List<Article>,
    val source: FeedSource,
)

/**
 * Where a [TopHeadlinesFeed] originated.
 */
enum class FeedSource {
    /**
     * The articles are the latest response from the network and have just been
     * persisted to the local cache.
     */
    NETWORK,

    /**
     * The network is unavailable and the articles were served from the
     * persistent local cache as a fallback.
     */
    CACHE,
}
