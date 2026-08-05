package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

/**
 * Pure formatting helpers shared by the Top Headlines card components.
 *
 * Kept as plain functions so they can be unit-tested in common test code
 * without any Compose runtime on the classpath.
 *
 * [sourceInitials] derives a short badge from a source name: the leading
 * capitals of the first word when it is an acronym (for example "BBC" -> "BB"),
 * otherwise its first letter, then the first letter of each following word,
 * capped at two characters. Original casing is preserved ("kotlin
 * Multiplatform" -> "kM"). Returns null for null, blank, or letter-less input.
 */
internal fun sourceInitials(sourceName: String?): String? {
    val words = sourceName?.trim()?.split(Regex("\\s+")).orEmpty()
    if (words.isEmpty() || words.first().isEmpty()) {
        return null
    }
    val firstWord = words.first()
    val leadingCapitals = firstWord.takeWhile { it.isUpperCase() }
    val initials =
        (if (leadingCapitals.isNotEmpty()) leadingCapitals else firstWord.first().toString()) +
            words.drop(1).mapNotNull { word -> word.firstOrNull()?.toString() }.joinToString("")
    return initials
        .takeIf { run -> run.any { it.isLetter() } }
        ?.take(2)
}
