package pl.recipesforsoftware.signalbrief.data.remote

/**
 * Injectable configuration for the shared NewsAPI client.
 *
 * [apiKey] is supplied at runtime by each platform's composition root (the Koin
 * Android composition reads `BuildConfig.NEWS_API_KEY`; iOS reads it from its `Info.plist`;
 * Desktop reads the `NEWS_API_KEY` environment variable).
 * It is never hard-coded in tracked files and is never included in failure
 * messages or logs.
 */
data class NewsApiConfig(
    val apiKey: String,
    val baseUrl: String,
)
