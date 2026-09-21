package pl.recipesforsoftware.signalbrief.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopConfigurationTest {
    @Test
    fun readNewsApiKeyReturnsTrimmedKey() {
        val key = readNewsApiKey(environment = mapOf("NEWS_API_KEY" to "  test-key  "))

        assertEquals("test-key", key)
    }

    @Test
    fun readNewsApiKeyFailsWhenKeyIsMissing() {
        val exception =
            assertFailsWith<IllegalStateException> { readNewsApiKey(environment = emptyMap()) }

        assertTrue(exception.message.orEmpty().contains("NEWS_API_KEY is missing or empty."))
    }

    @Test
    fun readNewsApiKeyFailsWhenKeyIsBlank() {
        val exception =
            assertFailsWith<IllegalStateException> {
                readNewsApiKey(environment = mapOf("NEWS_API_KEY" to "   "))
            }

        assertTrue(exception.message.orEmpty().contains("NEWS_API_KEY is missing or empty."))
    }

    @Test
    fun readNewsApiKeyFailureDoesNotExposeSecret() {
        val secret = "secret-value-that-must-not-be-exposed"
        val exception =
            assertFailsWith<IllegalStateException> {
                readNewsApiKey(environment = mapOf("UNRELATED_SECRET" to secret))
            }

        assertFalse(exception.message.orEmpty().contains(secret))
    }
}
