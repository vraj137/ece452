package com.appetizers.spotra.presentation

/**
 * Converts failures into text that is safe to render in the app.
 * Backend SDK exceptions can contain request URLs and authorization headers,
 * so their raw messages must never cross the presentation boundary.
 */
fun Throwable.toUserMessage(fallback: String): String {
    val candidate = (this as? IllegalArgumentException)?.message?.trim()
    return candidate
        ?.takeIf { it.length in 1..160 }
        ?.takeIf { '\n' !in it && '\r' !in it }
        ?.takeUnless { message ->
            val normalized = message.lowercase()
            SENSITIVE_ERROR_MARKERS.any(normalized::contains)
        }
        ?: fallback
}

private val SENSITIVE_ERROR_MARKERS = listOf(
    "authorization",
    "bearer",
    "header",
    "http://",
    "https://",
    "jwt",
    "rest/v1",
    "rpc/",
    "supabase.co",
    "url:"
)
