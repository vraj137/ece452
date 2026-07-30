package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.data.location.LocationRepository
import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.EmailInviteResult
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotOccupancy
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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

    private fun buildViewModel(home: HomeRepository = FakeHomeRepository()) = HomeViewModel(
        repository = home,
        authRepository = NullAuthRepository(),
        streakRepository = NoOpStreakRepository(),
        reviewRepository = NoOpReviewRepository(),
        awardBadgesUseCase = AwardBadgesUseCase(NoOpBadgeRepository(), NoOpReviewRepository()),
        locationRepository = NoOpLocationRepository(),
        friendRepository = NoOpFriendRepository(),
    )

    @Test
    fun `loads home snapshot into ui state`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Vraj", state.userFirstName)
        assertEquals("E7 Study Hall", state.soloSpot?.name)
        assertEquals(2, state.groupSession?.members?.size)
    }

    @Test
    fun `map spots load and default selection is solo spot`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.mapSpots.size)
        assertEquals("e7-study-hall", state.selectedSpotId)
    }

    @Test
    fun `occupancy broadcasts update loaded spot state`() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        repository.occupancyUpdates.emit(
            SpotOccupancy(
                spotId = "e7-study-hall",
                activeCount = 18,
                capacity = 20,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(90, state.soloSpot?.occupancyPercent)
        assertEquals("Busy", state.soloSpot?.badge)
        assertEquals(90, state.mapSpots.first { it.id == "e7-study-hall" }.occupancyPercent)
        assertEquals(18, state.occupancyBySpot["e7-study-hall"]?.activeCount)
    }

    @Test
    fun `review stats and cover photos reach the map spots`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        val spots = viewModel.uiState.value.mapSpots
        val rated = spots.single { it.id == "e7-study-hall" }
        assertEquals(4.8, rated.rating!!, 0.0001)
        assertEquals(12, rated.reviewCount)
        assertEquals("https://example.test/e7.jpg", rated.photoUrl)

        // A spot nobody has reviewed or photographed stays null rather than defaulting to zero
        // stars, which is what lets Explore label it "New" instead of ranking it worst.
        val unrated = spots.single { it.id == "dc-library" }
        assertNull(unrated.rating)
        assertEquals(0, unrated.reviewCount)
        assertNull(unrated.photoUrl)
    }

    @Test
    fun `live load failure stops loading and exposes retry state`() = runTest(dispatcher) {
        val viewModel = buildViewModel(FailingHomeRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.soloSpot)
        assertEquals(
            "We couldn't load live places. Check your connection and try again.",
            state.loadError
        )
    }

    @Test
    fun `selectMapSpot updates selected spot id`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectMapSpot("dc-library")

        assertEquals("dc-library", viewModel.uiState.value.selectedSpotId)
    }

    @Test
    fun `selectSection updates active home section`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectSection(HomeSection.Explore)

        val state = viewModel.uiState.value
        assertEquals(HomeSection.Explore, state.selectedSection)
        assertEquals(StudyMode.Solo, state.selectedMode)
    }

    @Test
    fun `selectSocialTab opens social section and selects tab`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.selectSocialTab(SocialTab.Discover)

        val state = viewModel.uiState.value
        assertEquals(HomeSection.Social, state.selectedSection)
        assertEquals(SocialTab.Discover, state.selectedSocialTab)
    }

    @Test
    fun `group invite appends member and clears input`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
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
    fun `creates a named group only after the user submits a valid name`() = runTest(dispatcher) {
        val repository = FakeHomeRepository(initialGroupSession = null)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.groupSession)

        viewModel.updateGroupName("  CS 341 Finals Crew  ")
        viewModel.createGroup()
        advanceUntilIdle()

        assertEquals("CS 341 Finals Crew", repository.lastCreatedGroupName)
        assertEquals(GroupVisibility.Private, repository.lastCreatedVisibility)
        assertEquals("CS 341 Finals Crew", viewModel.uiState.value.groupSession?.title)
        assertEquals("", viewModel.uiState.value.groupName)
        assertTrue(viewModel.uiState.value.groupSession?.isOwner == true)
    }

    @Test
    fun `creates a discoverable public group when public is selected`() = runTest(dispatcher) {
        val repository = FakeHomeRepository(initialGroupSession = null)
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.updateGroupName("Open Algorithms Study")
        viewModel.selectGroupVisibility(GroupVisibility.Public)
        viewModel.createGroup()
        advanceUntilIdle()

        assertEquals(GroupVisibility.Public, repository.lastCreatedVisibility)
        assertEquals(GroupVisibility.Public, viewModel.uiState.value.groupSession?.visibility)
    }

    @Test
    fun `joins a selected public group instead of treating discovery as membership`() = runTest(dispatcher) {
        val openGroup = GroupStudySession(
            id = "public-1",
            title = "Meet new study buddies",
            subtitle = "Open study group",
            proximityLabel = "",
            members = emptyList(),
            visibility = GroupVisibility.Public,
        )
        val repository = FakeHomeRepository(
            initialGroupSession = null,
            initialPublicGroups = listOf(openGroup)
        )
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.groupSession)
        assertEquals(listOf("public-1"), viewModel.uiState.value.publicGroups.map { it.id })

        viewModel.joinPublicGroup("public-1")
        advanceUntilIdle()

        assertEquals("public-1", repository.lastJoinedGroupId)
        assertEquals("public-1", viewModel.uiState.value.groupSession?.id)
        assertTrue(viewModel.uiState.value.publicGroups.isEmpty())
    }

    @Test
    fun `leaving clears the active group`() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        viewModel.leaveGroup()
        advanceUntilIdle()

        assertEquals("group-1", repository.lastLeftGroupId)
        assertNull(viewModel.uiState.value.groupSession)
    }

    @Test
    fun `check in and check out update active session`() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()

        val spot = requireNotNull(viewModel.uiState.value.soloSpot)
        viewModel.startCheckIn(spot, StudyMode.Solo)
        advanceUntilIdle()

        assertEquals("session-e7-study-hall", viewModel.uiState.value.activeCheckIn?.id)
        assertNull(repository.lastStartGroupSessionId)
        assertTrue(viewModel.uiState.value.showLiveSession)

        var checkedOutSpotId: String? = null
        viewModel.checkOut { session ->
            checkedOutSpotId = session.spot.id
        }
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeCheckIn)
        assertFalse(viewModel.uiState.value.showLiveSession)
        assertEquals("e7-study-hall", checkedOutSpotId)
        assertEquals(1, viewModel.uiState.value.completedSessions.size)
        assertEquals("E7 Study Hall", viewModel.uiState.value.completedSessions.first().spotName)
        assertTrue(viewModel.uiState.value.completedSessions.first().durationSeconds >= 0)
    }

    @Test
    fun `successful group check in uses group session and invokes success callback`() = runTest(dispatcher) {
        val repository = FakeHomeRepository()
        val viewModel = buildViewModel(repository)
        advanceUntilIdle()
        var succeeded = false

        viewModel.startCheckIn(
            requireNotNull(viewModel.uiState.value.soloSpot),
            StudyMode.Group,
            onSuccess = { succeeded = true }
        )
        advanceUntilIdle()

        assertTrue(succeeded)
        assertEquals("group-1", repository.lastStartGroupSessionId)
        assertEquals(StudyMode.Group, viewModel.uiState.value.activeCheckIn?.mode)
    }

    @Test
    fun `buddy request records requested student`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.sendBuddyRequest("akshat")
        advanceUntilIdle()

        assertTrue("akshat" in viewModel.uiState.value.requestedBuddyIds)
    }
}

private class FailingHomeRepository : HomeRepository {
    private fun failed(): Nothing = error("Supabase query failed")
    override suspend fun loadHome(): HomeSnapshot = failed()
    override suspend fun spotDetail(spotId: String): StudySpotDetail = failed()
    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = failed()
    override suspend fun createGroup(
        title: String,
        visibility: GroupVisibility
    ): GroupStudySession = failed()
    override suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession = failed()
    override suspend fun leaveGroup(groupSessionId: String) = failed()
    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?,
    ): CheckInSession = failed()
    override suspend fun checkOut(sessionId: String) = failed()
    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): EmailInviteResult = failed()
    override suspend fun inviteToGroupByUserId(groupSessionId: String, userId: String): GroupMember = failed()
    override suspend fun respondToGroupInvite(inviteId: String, accept: Boolean) = failed()
}

private class FakeHomeRepository(
    initialGroupSession: GroupStudySession? = defaultGroupSession(),
    initialPublicGroups: List<GroupStudySession> = emptyList(),
) : HomeRepository {
    val occupancyUpdates = MutableSharedFlow<SpotOccupancy>(extraBufferCapacity = 1)
    var lastStartGroupSessionId: String? = null
        private set
    var lastCreatedGroupName: String? = null
        private set
    var lastCreatedVisibility: GroupVisibility? = null
        private set
    var lastJoinedGroupId: String? = null
        private set
    var lastLeftGroupId: String? = null
        private set
    private var activeGroupSession: GroupStudySession? = initialGroupSession
    private var publicGroups: List<GroupStudySession> = initialPublicGroups

    override fun observeOccupancy() = occupancyUpdates

    private val soloSpot = StudySpotSummary(
        id = "e7-study-hall",
        name = "E7 Study Hall",
        badge = "Quiet",
        distanceMeters = 120,
        rating = 4.8,
        reviewCount = 12,
        photoUrl = "https://example.test/e7.jpg",
        studyContextLabel = "Solo-friendly",
        latitude = 43.4732,
        longitude = -80.5388
    )

    /** Deliberately unrated and photo-less: exercises the "no reviews / no photo yet" path. */
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
            groupSession = activeGroupSession,
            groupSpots = emptyList(),
            publicGroups = publicGroups,
            mapSpots = listOf(soloSpot, secondaryMapSpot)
        )

    override suspend fun spotDetail(spotId: String): StudySpotDetail =
        StudySpotDetail(
            id = spotId,
            name = soloSpot.name,
            building = "Engineering 7",
            floor = "Room 2101",
            badge = soloSpot.badge,
            distanceMeters = soloSpot.distanceMeters,
            rating = soloSpot.rating,
            studyContextLabel = soloSpot.studyContextLabel,
            noiseLevel = "Low",
            lighting = "Good",
            capacity = 24,
            occupancyPercent = 26,
            occupancyPercentIsLive = true,
            reportedOccupancyPercent = 26,
            peopleHere = 6,
            latitude = soloSpot.latitude,
            longitude = soloSpot.longitude
        )

    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = emptyList()

    override suspend fun createGroup(
        title: String,
        visibility: GroupVisibility
    ): GroupStudySession {
        lastCreatedGroupName = title
        lastCreatedVisibility = visibility
        return GroupStudySession(
            id = "created-group",
            title = title,
            subtitle = "Pick a spot and invite your friends",
            proximityLabel = "",
            members = listOf(GroupMember("you", "You", "VB")),
            isOwner = true,
            visibility = visibility,
        ).also { activeGroupSession = it }
    }

    override suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession {
        lastJoinedGroupId = groupSessionId
        val group = publicGroups.first { it.id == groupSessionId }.copy(
            members = listOf(GroupMember("you", "You", "VB")),
            isOwner = false,
        )
        activeGroupSession = group
        publicGroups = publicGroups.filterNot { it.id == groupSessionId }
        return group
    }

    override suspend fun leaveGroup(groupSessionId: String) {
        lastLeftGroupId = groupSessionId
        activeGroupSession = null
    }

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?,
    ): CheckInSession {
        lastStartGroupSessionId = groupSessionId
        return CheckInSession(
            id = "session-$spotId",
            spot = soloSpot,
            mode = mode,
            attendees = listOf(
                CheckedInStudent("you", "VB", "You (Vraj)", "CS 341 - studying now", isSelf = true)
            )
        )
    }

    override suspend fun checkOut(sessionId: String) = Unit

    override suspend fun inviteToGroup(
        groupSessionId: String,
        inviteText: String
    ): EmailInviteResult =
        EmailInviteResult.Added(GroupMember("invite-2", inviteText, "MR"))

    override suspend fun inviteToGroupByUserId(
        groupSessionId: String,
        userId: String
    ): GroupMember = GroupMember(userId, userId, userId.take(2).uppercase())

    override suspend fun respondToGroupInvite(inviteId: String, accept: Boolean) = Unit

    companion object {
        private fun defaultGroupSession() = GroupStudySession(
            id = "group-1",
            title = "app-etizers study sesh",
            subtitle = "CS 341 finals prep",
            proximityLabel = "all within 10 min",
            members = listOf(
                GroupMember("you", "Vraj Patel", "VB"),
                GroupMember("akshat", "Akshat J.", "AJ")
            ),
            isOwner = true,
        )
    }
}

private class NullAuthRepository : AuthRepository {
    override suspend fun currentUser(): AuthUser? = null
    override suspend fun sendOtp(email: String, createUser: Boolean) = Unit
    override suspend fun verifyOtp(email: String, token: String): AuthUser = error("not used")
    override suspend fun signOut() = Unit
}

private class NoOpStreakRepository : StreakRepository {
    override suspend fun recordLogin(userId: String) = 0
    override suspend fun recordCheckout(userId: String, spotId: String, spotName: String, durationSeconds: Int) = 0
    override suspend fun fetchRecentSessions(userId: String): List<CompletedSession> = emptyList()
}

private class NoOpLocationRepository : LocationRepository {
    override suspend fun getLastLocation(): Pair<Double, Double> =
        MapConfig.CAMPUS_LAT to MapConfig.CAMPUS_LNG
}

private class NoOpFriendRepository : FriendRepository {
    override suspend fun currentUserId(): String? = null
    override suspend fun fetchFriendProfiles(): List<FriendProfile> = emptyList()
    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> = emptyList()
    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> = emptyList()
    override suspend fun sendRequest(toUserId: String) = Unit
    override suspend fun acceptRequest(friendshipId: String) = Unit
    override suspend fun declineRequest(friendshipId: String) = Unit
    override suspend fun removeFriendship(friendshipId: String) = Unit
    override suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile> = emptyList()
}

private class NoOpBadgeRepository : BadgeRepository {
    override suspend fun getBadges(userId: String): List<UserBadge> = emptyList()
    override suspend fun awardBadge(userId: String, badgeId: BadgeId) = Unit
}

private class NoOpReviewRepository : ReviewRepository {
    override suspend fun reviewsFor(spotSlug: String): List<Review> = emptyList()
    override suspend fun submit(draft: ReviewDraft) = Unit
    override suspend fun update(reviewId: String, draft: ReviewDraft) = Unit
    override suspend fun delete(reviewId: String) = Unit
    override suspend fun getReviewCount(userId: String) = 0
    override suspend fun getQualityReviewCount(userId: String) = 0
}
