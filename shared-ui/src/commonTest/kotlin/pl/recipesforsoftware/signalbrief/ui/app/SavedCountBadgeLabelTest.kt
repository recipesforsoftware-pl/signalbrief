package pl.recipesforsoftware.signalbrief.ui.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedCountBadgeLabelTest {
    @Test
    fun `zero saved articles hides the badge`() {
        assertNull(savedCountBadgeLabel(0))
    }

    @Test
    fun `one saved article displays its exact count`() {
        assertEquals("1", savedCountBadgeLabel(1))
    }

    @Test
    fun `normal saved article count displays its exact count`() {
        assertEquals("8", savedCountBadgeLabel(8))
    }

    @Test
    fun `maximum visible saved article count displays its exact count`() {
        assertEquals("99", savedCountBadgeLabel(99))
    }

    @Test
    fun `one hundred saved articles displays a capped count`() {
        assertEquals("99+", savedCountBadgeLabel(100))
    }

    @Test
    fun `large saved article count displays a capped count`() {
        assertEquals("99+", savedCountBadgeLabel(157))
    }
}
