package pl.recipesforsoftware.signalbrief.desktop

private const val NEWS_API_KEY_ENVIRONMENT_VARIABLE = "NEWS_API_KEY"

internal fun readNewsApiKey(environment: Map<String, String> = System.getenv()): String =
    environment[NEWS_API_KEY_ENVIRONMENT_VARIABLE]?.trim().takeUnless { it.isNullOrEmpty() }
        ?: error(
            "NEWS_API_KEY is missing or empty. " +
                "Set the NEWS_API_KEY environment variable before starting SignalBrief Desktop.",
        )
