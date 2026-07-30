package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary

object SpotAttributeOptions {
    val NOISE = listOf("Silent", "Low", "Moderate", "Lively")
    val LIGHTING = listOf("Poor", "Good", "Bright", "Natural")
    val WIFI = listOf("Poor", "OK", "Good", "Fast")
    val OCCUPANCY = listOf("Empty", "Some", "Busy", "Packed")
}

data class SpotFilters(
    val noise: String? = null,
    val lighting: String? = null,
    val wifi: String? = null,
    val spaceType: StudyMode? = null,
    val amenity: String? = null,
    val maximumOccupancyPercent: Int? = null,
)

fun filterStudySpots(
    spots: List<StudySpotSummary>,
    filters: SpotFilters,
): List<StudySpotSummary> = spots.filter { spot ->
    matches(filters.noise, spot.noiseLevel) &&
        matches(filters.lighting, spot.lighting) &&
        matches(filters.wifi, spot.wifiQuality) &&
        when (filters.spaceType) {
            StudyMode.Solo -> spot.soloFriendly
            StudyMode.Group -> spot.groupFriendly
            null -> true
        } &&
        (
            filters.amenity == null ||
                spot.amenities.any { it.contains(filters.amenity, ignoreCase = true) }
            ) &&
        (
            filters.maximumOccupancyPercent == null ||
                spot.occupancyPercent?.let { it <= filters.maximumOccupancyPercent } == true
            )
}

private fun matches(filter: String?, value: String?): Boolean =
    filter == null || value?.equals(filter, ignoreCase = true) == true
