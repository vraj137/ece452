package com.appetizers.spotra.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotReviewAggregatesTest {

    @Test
    fun `averages ratings to a single decimal`() {
        val result = SpotReviewAggregator.aggregate(
            ratings = listOf(5, 4, 4),
            noiseLevels = emptyList(),
            lightings = emptyList(),
            wifiQualities = emptyList(),
        )

        assertEquals(4.3, result.averageRating!!, 0.0001)
        assertEquals(3, result.reviewCount)
    }

    @Test
    fun `rounds half up the same way postgres round does`() {
        val result = SpotReviewAggregator.aggregate(
            ratings = listOf(4, 5),
            noiseLevels = emptyList(),
            lightings = emptyList(),
            wifiQualities = emptyList(),
        )

        assertEquals(4.5, result.averageRating!!, 0.0001)
    }

    @Test
    fun `no reviews yields a null rating and zero count`() {
        val result = SpotReviewAggregator.aggregate(
            ratings = emptyList(),
            noiseLevels = emptyList(),
            lightings = emptyList(),
            wifiQualities = emptyList(),
        )

        assertNull(result.averageRating)
        assertEquals(0, result.reviewCount)
        assertNull(result.noiseLevel)
        assertNull(result.lighting)
        assertNull(result.wifiQuality)
    }

    @Test
    fun `modal label picks the most reported value`() {
        assertEquals("Silent", SpotReviewAggregator.modalLabel(listOf("Moderate", "Silent", "Silent")))
    }

    @Test
    fun `modal label breaks ties alphabetically so ordering is stable`() {
        // "Moderate" and "Silent" both appear twice; the lower label wins in both Kotlin and the
        // spot_review_stats view, so a reload never reshuffles the leaderboard.
        assertEquals(
            "Moderate",
            SpotReviewAggregator.modalLabel(listOf("Silent", "Moderate", "Silent", "Moderate"))
        )
    }

    @Test
    fun `modal label ignores nulls and blanks`() {
        assertEquals("Bright", SpotReviewAggregator.modalLabel(listOf(null, "", "  ", "Bright")))
        assertNull(SpotReviewAggregator.modalLabel(listOf(null, "", "   ")))
    }

    @Test
    fun `aggregates every facet independently`() {
        val result = SpotReviewAggregator.aggregate(
            ratings = listOf(3, 5),
            noiseLevels = listOf("Lively", "Lively", "Silent"),
            lightings = listOf(null, "Natural"),
            wifiQualities = listOf("Fast", "Fast", "Spotty"),
        )

        assertEquals(4.0, result.averageRating!!, 0.0001)
        assertEquals("Lively", result.noiseLevel)
        assertEquals("Natural", result.lighting)
        assertEquals("Fast", result.wifiQuality)
    }
}
