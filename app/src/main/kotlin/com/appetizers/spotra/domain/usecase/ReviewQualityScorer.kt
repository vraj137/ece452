package com.appetizers.spotra.domain.usecase

object ReviewQualityScorer {
    private val keywordGroups = listOf(
        listOf("quiet", "silent", "loud", "noisy", "noise"),
        listOf("wifi", "internet", "connection", "signal"),
        listOf("crowded", "busy", "empty", "packed", "available"),
        listOf("outlet", "plug", "charge", "charging", "power"),
        listOf("lighting", "bright", "dark", "light"),
        listOf("clean", "dirty", "maintained"),
    )

    fun score(comment: String?): Int {
        if (comment.isNullOrBlank()) return 0
        val text = comment.lowercase()
        val lengthScore = when {
            comment.length >= 200 -> 4
            comment.length >= 100 -> 3
            comment.length >= 50  -> 2
            comment.length >= 20  -> 1
            else -> 0
        }
        val keywordScore = keywordGroups.count { group -> group.any { text.contains(it) } }
        return minOf(lengthScore + keywordScore, 10)
    }
}
