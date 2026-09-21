package pl.recipesforsoftware.signalbrief.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionNameTest {
    @Test
    fun validNameIsTrimmed() {
        val name = CollectionName.from("  Reading list  ")
        assertTrue(name != null)
        assertEquals("Reading list", name.value)
    }

    @Test
    fun blankOnlyNameIsRejected() {
        assertNull(CollectionName.from("   "))
        assertNull(CollectionName.from(""))
        assertNull(CollectionName.from("\t\n "))
    }

    @Test
    fun nameWithoutWhitespaceIsUnchanged() {
        val name = CollectionName.from("Reading")
        assertTrue(name != null)
        assertEquals("Reading", name.value)
    }

    @Test
    fun namesTrimmedToSameValueNormalizeTheSame() {
        assertEquals(
            CollectionName.from("Startups")?.value,
            CollectionName.from("  Startups  ")?.value,
        )
    }
}
