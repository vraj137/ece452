package com.appetizers.spotra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelsTest {

    @Test
    fun `zero active check-ins is live zero percent without configured capacity`() {
        val occupancy = SpotOccupancy(
            spotId = "dp-individual-carrels-floors-6-9",
            activeCount = 0,
            capacity = null,
        )

        assertEquals(0, occupancy.percent)
    }

    @Test
    fun `detail never substitutes historical review occupancy for live occupancy`() {
        val detail = StudySpotDetail(
            id = "test-spot",
            name = "Test Spot",
            building = "Test Building",
            badge = "Quiet",
            occupancyPercent = 38,
            occupancyPercentIsLive = false,
        )

        val updated = detail.withOccupancy(
            SpotOccupancy(
                spotId = "test-spot",
                activeCount = 3,
                capacity = null,
            )
        )

        assertNull(updated.occupancyPercent)
        assertFalse(updated.occupancyPercentIsLive)
        assertEquals(3, updated.peopleHere)
    }

    @Test
    fun `zero active check-ins replaces a historical report with live zero`() {
        val detail = StudySpotDetail(
            id = "test-spot",
            name = "Test Spot",
            building = "Test Building",
            badge = "Moderate",
            occupancyPercent = 38,
            occupancyPercentIsLive = false,
        )

        val updated = detail.withOccupancy(
            SpotOccupancy(
                spotId = "test-spot",
                activeCount = 0,
                capacity = null,
            )
        )

        assertEquals(0, updated.occupancyPercent)
        assertTrue(updated.occupancyPercentIsLive)
        assertEquals(0, updated.peopleHere)
    }
}
