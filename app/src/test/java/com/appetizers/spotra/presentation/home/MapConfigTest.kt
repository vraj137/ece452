package com.appetizers.spotra.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapConfigTest {

    @Test
    fun `coerceZoom clamps below the minimum`() {
        assertEquals(MapConfig.MIN_ZOOM, MapConfig.coerceZoom(5.0), 0.0)
    }

    @Test
    fun `coerceZoom clamps above the maximum`() {
        assertEquals(MapConfig.MAX_ZOOM, MapConfig.coerceZoom(99.0), 0.0)
    }

    @Test
    fun `coerceZoom passes an in-range value through unchanged`() {
        assertEquals(15.0, MapConfig.coerceZoom(15.0), 0.0)
    }

    @Test
    fun `zoom constants are ordered min less than default less than max`() {
        assertTrue(MapConfig.MIN_ZOOM < MapConfig.DEFAULT_ZOOM)
        assertTrue(MapConfig.DEFAULT_ZOOM < MapConfig.MAX_ZOOM)
    }

    @Test
    fun `mapDisplayState is PLACEHOLDER when the token is blank`() {
        assertEquals(MapDisplayState.PLACEHOLDER, mapDisplayState(tokenBlank = true, locatedCount = 4))
    }

    @Test
    fun `mapDisplayState is EMPTY when token present but no located spots`() {
        assertEquals(MapDisplayState.EMPTY, mapDisplayState(tokenBlank = false, locatedCount = 0))
    }

    @Test
    fun `mapDisplayState is CONTENT when token present and spots located`() {
        assertEquals(MapDisplayState.CONTENT, mapDisplayState(tokenBlank = false, locatedCount = 3))
    }

    @Test
    fun `buildingSpacesDisplayState prioritizes loading over everything`() {
        assertEquals(
            BuildingSpacesDisplayState.LOADING,
            buildingSpacesDisplayState(isLoading = true, error = "boom", spaceCount = 5)
        )
    }

    @Test
    fun `buildingSpacesDisplayState reports error when not loading`() {
        assertEquals(
            BuildingSpacesDisplayState.ERROR,
            buildingSpacesDisplayState(isLoading = false, error = "boom", spaceCount = 5)
        )
    }

    @Test
    fun `buildingSpacesDisplayState is EMPTY when loaded with no spaces`() {
        assertEquals(
            BuildingSpacesDisplayState.EMPTY,
            buildingSpacesDisplayState(isLoading = false, error = null, spaceCount = 0)
        )
    }

    @Test
    fun `buildingSpacesDisplayState is CONTENT when loaded with spaces`() {
        assertEquals(
            BuildingSpacesDisplayState.CONTENT,
            buildingSpacesDisplayState(isLoading = false, error = null, spaceCount = 2)
        )
    }
}
