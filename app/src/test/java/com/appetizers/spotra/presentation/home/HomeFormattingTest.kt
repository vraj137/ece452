package com.appetizers.spotra.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFormattingTest {
    @Test
    fun `formats campus distance as walking time`() {
        assertEquals("4 min walk", studySpotDistanceLabel(340, null))
    }

    @Test
    fun `formats longer nearby distance in kilometres`() {
        assertEquals("3.5 km away", studySpotDistanceLabel(3_540, null))
    }

    @Test
    fun `does not show absurd walking time for a remote emulator location`() {
        assertEquals("Solo or group", studySpotDistanceLabel(3_541_000, "Solo or group"))
        assertEquals("", studySpotDistanceLabel(3_541_000, null))
    }
}

