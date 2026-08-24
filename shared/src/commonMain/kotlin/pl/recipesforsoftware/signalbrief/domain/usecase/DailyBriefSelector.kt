package pl.recipesforsoftware.signalbrief.domain.usecase

import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.DailyBrief

/**
 * Builds a predictable Daily Brief from locally cached headlines.
 *
 * Articles must have an HTTP(S) URL to be eligible. The first occurrence of
 * each exact URL wins, source order is retained, and no more than [MAX_ARTICLES]
 * articles are returned. This use case has no repository dependency, so it
 * cannot refresh or otherwise access a remote source.
 */
class DailyBriefSelector {
    operator fun invoke(cachedHeadlines: List<Article>): DailyBrief {
        val seenUrls = mutableSetOf<String>()
        val selected =
            cachedHeadlines
                .asSequence()
                .filter { it.hasActionableUrl() }
                .filter { seenUrls.add(it.url) }
                .take(MAX_ARTICLES)
                .toList()
        return DailyBrief(articles = selected)
    }

    private fun Article.hasActionableUrl(): Boolean {
        val trimmedUrl = url.trim()
        return trimmedUrl.startsWith(prefix = HTTP_PREFIX, ignoreCase = true) ||
            trimmedUrl.startsWith(prefix = HTTPS_PREFIX, ignoreCase = true)
    }

    private companion object {
        const val MAX_ARTICLES = 5
        const val HTTP_PREFIX = "http://"
        const val HTTPS_PREFIX = "https://"
    }
}
