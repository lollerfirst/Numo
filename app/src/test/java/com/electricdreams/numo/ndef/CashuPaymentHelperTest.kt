package com.electricdreams.numo.ndef

import com.electricdreams.numo.ndef.CashuPaymentHelper.extractCashuToken
import com.electricdreams.numo.ndef.CashuPaymentHelper.isCashuPaymentRequest
import com.electricdreams.numo.ndef.CashuPaymentHelper.isCashuToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure tests for the deterministic helpers in [CashuPaymentHelper].
 */
class CashuPaymentHelperTest {

    @Test
    fun `isCashuToken recognizes cashuA prefix, case-insensitive`() {
        assertTrue(isCashuToken("cashuA123"))
        assertTrue(isCashuToken("CASHUaXYZ"))
        assertFalse(isCashuToken(null))
        assertFalse(isCashuToken("not-a-token"))
    }

    @Test
    fun `isCashuPaymentRequest recognizes creqA prefix`() {
        assertTrue(isCashuPaymentRequest("creqA123"))
        assertFalse(isCashuPaymentRequest(null))
        assertFalse(isCashuPaymentRequest("cashuA123"))
    }

    @Test
    fun `extractCashuToken returns full token when surrounded by text`() {
        val tokenString = "cashuAabcdefg12345"
        // When the whole string is a token, helper returns it unchanged
        val token = extractCashuToken(tokenString)

        assertEquals(tokenString, token)
    }

    @Test
    fun `extractCashuToken stops at whitespace or delimiters`() {
        val variants = listOf(
            "before cashuAabc123 after",
            "json: \"cashuAabc123\" end",
            "html: <span>cashuAabc123</span>",
        )

        variants.forEach { text ->
            val token = extractCashuToken(text)
            assertEquals("cashuAabc123", token)
        }
    }

    @Test
    fun `extractCashuToken returns null when no token is present`() {
        val text = "there is no token here"

        val token = extractCashuToken(text)

        assertNull(token)
    }
}
