package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.StudyMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugHomeRepositoryTest {

    @Test
    fun `loadHome returns map spots with coordinates`() = runTest {
        val snapshot = DebugHomeRepository().loadHome()

        assertEquals(6, snapshot.mapSpots.size)
        assertTrue(
            "every map spot must have coordinates",
            snapshot.mapSpots.all { it.latitude != null && it.longitude != null }
        )
    }

    @Test
    fun `solo spot is included among map spots`() = runTest {
        val snapshot = DebugHomeRepository().loadHome()

        assertTrue(
            "solo spot should be one of the map markers",
            snapshot.mapSpots.any { it.id == snapshot.soloSpot.id }
        )
    }

    @Test
    fun `can check in to a map-only spot`() = runTest {
        val repo = DebugHomeRepository()
        val snapshot = repo.loadHome()
        val mapOnlySpot = snapshot.mapSpots.first { it.id != snapshot.soloSpot.id }

        val session = repo.startCheckIn(mapOnlySpot.id, StudyMode.Solo, null)

        assertEquals(mapOnlySpot.id, session.spot.id)
    }
}
