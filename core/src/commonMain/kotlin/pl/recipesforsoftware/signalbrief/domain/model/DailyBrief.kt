package pl.recipesforsoftware.signalbrief.domain.model

/**
 * A deterministic, locally derived set of articles for the Daily Brief reader.
 *
 * This foundation is intentionally date-agnostic: it is built from the latest
 * cached headline order until a safe shared date/time policy is introduced.
 */
data class DailyBrief(
    val articles: List<Article>,
)
