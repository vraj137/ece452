package com.appetizers.spotra.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class UserFacingErrorsTest {
    @Test
    fun `backend exception details are replaced with fallback`() {
        val error = IllegalStateException(
            "Headers: Authorization=Bearer sensitive-token URL: https://project.supabase.co/rest/v1/rpc/example"
        )

        assertEquals("Could not load data.", error.toUserMessage("Could not load data."))
    }

    @Test
    fun `safe validation messages can be shown`() {
        val error = IllegalArgumentException("Enter the 6-digit code from your email.")

        assertEquals(
            "Enter the 6-digit code from your email.",
            error.toUserMessage("Something went wrong.")
        )
    }

    @Test
    fun `multiline messages are never shown`() {
        val error = IllegalArgumentException("Request failed\nHeaders: redacted")

        assertEquals("Could not load data.", error.toUserMessage("Could not load data."))
    }
}
