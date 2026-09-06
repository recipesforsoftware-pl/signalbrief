package pl.recipesforsoftware.signalbrief.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TopicQueryTest {
    @Test
    fun validQueryIsTrimmedOfSurroundingWhitespace() {
        val query = TopicQuery.from("  Kotlin Multiplatform  ")
        assertTrue(query != null)
        assertEquals("Kotlin Multiplatform", query.value)
    }

    @Test
    fun blankOnlyQueryIsRejected() {
        assertNull(TopicQuery.from("   "))
        assertNull(TopicQuery.from(""))
        assertNull(TopicQuery.from("\t\n "))
    }

    @Test
    fun displayCasingIsPreserved() {
        val query = TopicQuery.from("Kotlin")
        assertTrue(query != null)
        assertEquals("Kotlin", query.value)
        assertEquals("kotlin", query.normalizedKey)
    }

    @Test
    fun queryWithoutWhitespaceIsUnchanged() {
        val query = TopicQuery.from("AI")
        assertTrue(query != null)
        assertEquals("AI", query.value)
    }

    @Test
    fun queryTrimmedToSameTextNormalizesTheSame() {
        assertEquals(
            TopicQuery.from("Startups")?.normalizedKey,
            TopicQuery.from("  Startups  ")?.normalizedKey,
        )
    }

    @Test
    fun duplicateComparisonIsCaseInsensitive() {
        val upper = TopicQuery.from("Kotlin")
        val lower = TopicQuery.from("kotlin")
        val mixed = TopicQuery.from("  KoTliN  ")

        assertTrue(upper != null && lower != null && mixed != null)
        assertEquals(upper.normalizedKey, lower.normalizedKey)
        assertEquals(upper.normalizedKey, mixed.normalizedKey)
    }
}
