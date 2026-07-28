package com.appetizers.spotra.presentation.home

/**
 * Camera constants for the campus map. Single source of truth so the initial
 * viewport, the zoom-button clamp, and any future callers stay in sync.
 */
object MapConfig {
    const val CAMPUS_LNG: Double = -80.5430
    const val CAMPUS_LAT: Double = 43.4720
    const val DEFAULT_ZOOM: Double = 14.6
    const val MIN_ZOOM: Double = 13.0
    const val MAX_ZOOM: Double = 18.5

    private val supportedUwZones = listOf(
        CampusZone(43.460, 43.485, -80.565, -80.515), // Waterloo main campus
        CampusZone(43.445, 43.458, -80.510, -80.488), // School of Pharmacy
        CampusZone(43.350, 43.367, -80.330, -80.300), // School of Architecture
    )

    fun coerceZoom(z: Double): Double = z.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun isWithinSupportedUwCampus(latitude: Double, longitude: Double): Boolean =
        supportedUwZones.any { it.contains(latitude, longitude) }
}

private data class CampusZone(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
) {
    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in minLatitude..maxLatitude && longitude in minLongitude..maxLongitude
}

/** What the campus map should render given the token and available located spots. */
enum class MapDisplayState { PLACEHOLDER, EMPTY, CONTENT }

fun mapDisplayState(tokenBlank: Boolean, locatedCount: Int): MapDisplayState =
    when {
        tokenBlank -> MapDisplayState.PLACEHOLDER
        locatedCount == 0 -> MapDisplayState.EMPTY
        else -> MapDisplayState.CONTENT
    }

/** What the building-spaces screen should render for a given load state. */
enum class BuildingSpacesDisplayState { LOADING, ERROR, EMPTY, CONTENT }

fun buildingSpacesDisplayState(
    isLoading: Boolean,
    error: String?,
    spaceCount: Int
): BuildingSpacesDisplayState =
    when {
        isLoading -> BuildingSpacesDisplayState.LOADING
        error != null -> BuildingSpacesDisplayState.ERROR
        spaceCount == 0 -> BuildingSpacesDisplayState.EMPTY
        else -> BuildingSpacesDisplayState.CONTENT
    }
