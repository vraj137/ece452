package com.appetizers.spotra.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotNavigationTest {
    @Test
    fun backFromChildReturnsToItsParentSpot() {
        val parentPath = rootSpotPath("davis-library")
        val childPath = childSpotPath(parentPath, "davis-lower-quiet-study")

        assertEquals(parentPath, previousSpotPath(childPath))
    }

    @Test
    fun backFromRootReturnsToTheOriginatingHomeSection() {
        val rootPath = rootSpotPath("davis-library")

        assertEquals(emptyList<String>(), previousSpotPath(rootPath))
    }

    @Test
    fun openingAnotherRootClearsOldSpotHistory() {
        val oldChildPath = childSpotPath(
            rootSpotPath("davis-library"),
            "davis-lower-quiet-study"
        )

        assertEquals(listOf("quantum-nano-centre"), rootSpotPath("quantum-nano-centre"))
        assertEquals(
            listOf("davis-library", "davis-lower-quiet-study"),
            oldChildPath
        )
    }
}
