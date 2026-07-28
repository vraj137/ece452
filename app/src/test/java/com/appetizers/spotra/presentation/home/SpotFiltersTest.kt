package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class SpotFiltersTest {
    private val quietSolo = spot(
        id = "quiet",
        noise = "Silent",
        lighting = "Natural",
        wifi = "Fast",
        occupancy = 30,
        solo = true,
        group = false,
        amenities = listOf("Fast Wi-Fi", "Outlets"),
    )
    private val groupRoom = spot(
        id = "group",
        noise = "Moderate",
        lighting = "Bright",
        wifi = "Good",
        occupancy = 70,
        solo = false,
        group = true,
        amenities = listOf("Whiteboard"),
    )

    @Test
    fun `combines all selected filters`() {
        val result = filterStudySpots(
            listOf(quietSolo, groupRoom),
            SpotFilters(
                noise = "silent",
                lighting = "Natural",
                wifi = "Fast",
                spaceType = StudyMode.Solo,
                amenity = "Wi-Fi",
                maximumOccupancyPercent = 50,
            ),
        )

        assertEquals(listOf("quiet"), result.map { it.id })
    }

    @Test
    fun `occupancy filter excludes spots without live capacity data`() {
        val unknown = quietSolo.copy(id = "unknown", occupancyPercent = null)

        val result = filterStudySpots(
            listOf(unknown, quietSolo),
            SpotFilters(maximumOccupancyPercent = 50),
        )

        assertEquals(listOf("quiet"), result.map { it.id })
    }

    private fun spot(
        id: String,
        noise: String,
        lighting: String,
        wifi: String,
        occupancy: Int,
        solo: Boolean,
        group: Boolean,
        amenities: List<String>,
    ) = StudySpotSummary(
        id = id,
        name = id,
        badge = "",
        noiseLevel = noise,
        lighting = lighting,
        wifiQuality = wifi,
        occupancyPercent = occupancy,
        soloFriendly = solo,
        groupFriendly = group,
        amenities = amenities,
    )
}
