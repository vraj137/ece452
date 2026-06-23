package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.HomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads home snapshot into ui state`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Vraj", state.userFirstName)
        assertEquals("E7 Study Hall", state.soloSpot?.name)
        assertEquals(2, state.groupSession?.members?.size)
    }

    @Test
    fun `map spots load and default selection is solo spot`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.mapSpots.size)
        assertEquals("e7-study-hall", state.selectedSpotId)
    }

    @Test
    fun `selectMapSpot updates selected spot id`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        viewModel.selectMapSpot("dc-library")

        assertEquals("dc-library", viewModel.uiState.value.selectedSpotId)
    }

    @Test
    fun `selectSection updates active home section`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        viewModel.selectSection(HomeSection.Explore)

        val state = viewModel.uiState.value
        assertEquals(HomeSection.Explore, state.selectedSection)
        assertEquals(StudyMode.Solo, state.selectedMode)
    }

    @Test
    fun `selectSocialTab opens social section and selects tab`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        viewModel.selectSocialTab(SocialTab.Discover)

        val state = viewModel.uiState.value
        assertEquals(HomeSection.Social, state.selectedSection)
        assertEquals(SocialTab.Discover, state.selectedSocialTab)
    }

    @Test
    fun `group invite appends member and clears input`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        viewModel.updateInviteText("Maya R")
        viewModel.sendGroupInvite()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inviteText)
        assertEquals(3, state.groupSession?.members?.size)
        assertEquals("MR", state.groupSession?.members?.last()?.initials)
    }

    @Test
    fun `check in and check out update active session`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        val spot = requireNotNull(viewModel.uiState.value.soloSpot)
        viewModel.startCheckIn(spot, StudyMode.Solo)
        advanceUntilIdle()

        assertEquals("session-e7-study-hall", viewModel.uiState.value.activeCheckIn?.id)
        assertTrue(viewModel.uiState.value.showLiveSession)

        viewModel.checkOut()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeCheckIn)
        assertFalse(viewModel.uiState.value.showLiveSession)
        assertEquals(1, viewModel.uiState.value.completedSessions.size)
        assertEquals("E7 Study Hall", viewModel.uiState.value.completedSessions.first().spotName)
        assertTrue(viewModel.uiState.value.completedSessions.first().durationSeconds >= 0)
    }

    @Test
    fun `buddy request records requested student`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeHomeRepository())
        advanceUntilIdle()

        viewModel.sendBuddyRequest("akshat")
        advanceUntilIdle()

        assertTrue("akshat" in viewModel.uiState.value.requestedBuddyIds)
    }
}

private class FakeHomeRepository : HomeRepository {
    private val soloSpot = StudySpotSummary(
        id = "e7-study-hall",
        name = "E7 Study Hall",
        badge = "Quiet",
        distanceMeters = 120,
        rating = 4.8,
        studyContextLabel = "Solo-friendly",
        latitude = 43.4732,
        longitude = -80.5388
    )
    private val groupSession = GroupStudySession(
        id = "group-1",
        title = "app-etizers study sesh",
        subtitle = "CS 341 finals prep",
        proximityLabel = "all within 10 min",
        members = listOf(
            GroupMember("you", "Vraj Patel", "VB"),
            GroupMember("akshat", "Akshat J.", "AJ")
        )
    )

    private val secondaryMapSpot = StudySpotSummary(
        id = "dc-library",
        name = "DC Library",
        badge = "Quiet",
        latitude = 43.4728,
        longitude = -80.5424
    )

    override suspend fun loadHome(): HomeSnapshot =
        HomeSnapshot(
            userFirstName = "Vraj",
            soloSpot = soloSpot,
            groupSession = groupSession,
            groupSpots = emptyList(),
            mapSpots = listOf(soloSpot, secondaryMapSpot)
        )

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession =
        CheckInSession(
            id = "session-$spotId",
            spot = soloSpot,
            mode = mode,
            attendees = listOf(
                CheckedInStudent("you", "VB", "You (Vraj)", "CS 341 - studying now", isSelf = true)
            )
        )

    override suspend fun checkOut(sessionId: String) = Unit

    override suspend fun sendBuddyRequest(studentId: String) = Unit

    override suspend fun inviteToGroup(
        groupSessionId: String,
        inviteText: String
    ): GroupMember =
        GroupMember("invite-2", inviteText, "MR")
}
