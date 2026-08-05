package pl.recipesforsoftware.signalbrief.ui.topheadlines

import pl.recipesforsoftware.signalbrief.domain.model.Article

/**
 * Returns `true` when this article's [url] is safe to hand to a platform URI handler.
 *
 * The domain mapper already guarantees that [Article.url] is non-blank, but a
 * user-facing open action should still restrict itself to `http`/`https` schemes
 * so that unexpected or invalid URLs cannot crash the host app.
 */
fun Article.hasActionableUrl(): Boolean {
    val trimmed = url.trim()
    return trimmed.isNotBlank() &&
        (
            trimmed.startsWith(prefix = "http://", ignoreCase = true) ||
                trimmed.startsWith(prefix = "https://", ignoreCase = true)
        )
}
