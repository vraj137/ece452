package com.appetizers.spotra.domain.usecase

import java.math.BigDecimal
import java.math.RoundingMode

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
        averageRating = averageRatingOf(ratings),
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

    /**
     * Exact rational division rounded half-away-from-zero, which is what Postgres
     * `round(avg(rating)::numeric, 1)` does in the spot_review_stats view.
     *
     * Do not reach for `kotlin.math.round` here — it delegates to `Math.rint`, which rounds ties
     * to even, so an average of 4.25 becomes 4.2 in Kotlin and 4.3 in SQL. Verified against a
     * local Postgres. BigDecimal also avoids the binary-float trap where 4.35 * 10 evaluates to
     * 43.499999999999996 and rounds the wrong way.
     */
    private fun averageRatingOf(ratings: List<Int>): Double? {
        if (ratings.isEmpty()) return null
        return BigDecimal(ratings.sum())
            .divide(BigDecimal(ratings.size), 1, RoundingMode.HALF_UP)
            .toDouble()
    }
}
