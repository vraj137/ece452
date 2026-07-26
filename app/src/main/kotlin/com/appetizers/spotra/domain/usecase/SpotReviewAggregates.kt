package com.appetizers.spotra.domain.usecase

/**
 * Consensus view of a spot's reviews.
 *
 * Mirrors the `public.spot_review_stats` view created in
 * `supabase/migrations/20260726120000_create_spot_review_stats.sql`. The view feeds the Explore
 * leaderboards (one query for every spot); this object feeds the spot detail screen, which already
 * has the full review list in memory. Both must produce the same numbers, so the rules are: average
 * rounded to one decimal, and a modal label chosen by highest count with ties broken alphabetically
 * ascending. Change one, change the other.
 */
data class SpotReviewAggregates(
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
)

object SpotReviewAggregator {

    fun aggregate(
        ratings: List<Int>,
        noiseLevels: List<String?>,
        lightings: List<String?>,
        wifiQualities: List<String?>,
    ): SpotReviewAggregates = SpotReviewAggregates(
        averageRating = ratings.takeIf { it.isNotEmpty() }?.average()?.roundToSingleDecimal(),
        reviewCount = ratings.size,
        noiseLevel = modalLabel(noiseLevels),
        lighting = modalLabel(lightings),
        wifiQuality = modalLabel(wifiQualities),
    )

    /**
     * The label most reviewers reported. Ties break alphabetically so repeated loads of the same
     * data always render the same value.
     */
    fun modalLabel(labels: List<String?>): String? = labels
        .filterNot { it.isNullOrBlank() }
        .filterNotNull()
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .firstOrNull()
        ?.key

    private fun Double.roundToSingleDecimal(): Double = kotlin.math.round(this * 10.0) / 10.0
}
