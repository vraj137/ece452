package com.appetizers.spotra.presentation

import io.github.jan.supabase.exceptions.RestException

fun Throwable.toUserMessage(fallback: String): String {
    val candidate = when (this) {
        is IllegalArgumentException -> message?.trim()
        is RestException -> error.trim().ifEmpty { null }
        else -> null
    }
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
    "url:",
    "schema cache",
    "public.",
    "could not find the function",
    "p_spot_slug",
    "p_mode",
    "p_group_session_id",
)
