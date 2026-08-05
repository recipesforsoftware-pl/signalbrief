package com.recipesforsoftware.mvvm.ui.topheadlines.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the pure card formatting helpers. No Compose runtime is
 * involved, so these run on every target in common test code.
 */
class ArticleCardFormattingTest {
    @Test
    fun `single word source yields its first letter uppercased`() {
        assertEquals("R", sourceInitials("Reuters"))
    }

    @Test
    fun `multi word source yields up to two initial letters`() {
        assertEquals("BB", sourceInitials("BBC News"))
        assertEquals("FT", sourceInitials("Financial Times"))
    }

    @Test
    fun `three word source only keeps the first two initials`() {
        assertEquals("AD", sourceInitials("Android Developers Blog"))
    }

    @Test
    fun `initials preserve existing casing`() {
        assertEquals("kM", sourceInitials("kotlin Multiplatform"))
    }

    @Test
    fun `null source yields null initials`() {
        assertNull(sourceInitials(null))
    }

    @Test
    fun `blank source yields null initials`() {
        assertNull(sourceInitials("   "))
    }

    @Test
    fun `source without letters yields null initials`() {
        assertNull(sourceInitials("&"))
    }
}
