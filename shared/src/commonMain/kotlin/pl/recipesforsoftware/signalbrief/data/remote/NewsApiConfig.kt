package pl.recipesforsoftware.signalbrief.data.remote

/**
 * Injectable configuration for the shared NewsAPI client.
 *
 * [apiKey] is supplied at runtime by each platform's composition root (Hilt on
 * Android reads the existing `BuildConfig.NEWS_API_KEY`; iOS will provide its
 * own value later). It is never hard-coded in tracked files and is never
 * included in failure messages or logs.
 */
data class NewsApiConfig(
    val apiKey: String,
    val baseUrl: String,
)
