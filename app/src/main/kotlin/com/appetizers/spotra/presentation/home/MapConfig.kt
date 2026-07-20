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

    fun coerceZoom(z: Double): Double = z.coerceIn(MIN_ZOOM, MAX_ZOOM)
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
