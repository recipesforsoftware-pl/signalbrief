package pl.recipesforsoftware.signalbrief.ui.articledetails

/**
 * Centralized user-facing strings of the Article Details screen.
 *
 * A plain Kotlin object keeps the shared UI free from Android resource
 * dependencies while giving both hosts a single source of truth. Bookmark
 * semantics are intentionally reused from [pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesStrings]
 * so the save/remove wording stays identical across feed, Saved list, and
 * details.
 */
object ArticleDetailsStrings {
    const val BACK: String = "Back"
    const val READ_FULL_ARTICLE: String = "Read full article"
}
