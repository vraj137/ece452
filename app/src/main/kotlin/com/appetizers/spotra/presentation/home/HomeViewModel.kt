package com.appetizers.spotra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.data.location.LocationRepository
import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.EmailInviteResult
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.GroupInvite
import com.appetizers.spotra.domain.model.GroupSessionEvent
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotOccupancy
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.withOccupancy
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import com.appetizers.spotra.domain.usecase.ReviewQualityScorer
import com.appetizers.spotra.presentation.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val userFirstName: String = "",
    val selectedMode: StudyMode = StudyMode.Solo,
    val selectedSection: HomeSection = HomeSection.Map,
    val selectedSocialTab: SocialTab = SocialTab.Friends,
    val soloSpot: StudySpotSummary? = null,
    val groupSession: GroupStudySession? = null,
    val groupSpots: List<StudySpotSummary> = emptyList(),
    val publicGroups: List<GroupStudySession> = emptyList(),
    val mapSpots: List<StudySpotSummary> = emptyList(),
    val occupancyBySpot: Map<String, SpotOccupancy> = emptyMap(),
    val trendingCounts: Map<String, Int> = emptyMap(),
    val selectedSpotId: String? = null,
    val activeCheckIn: CheckInSession? = null,
    val sessionStartTimeMillis: Long = 0L,
    val showLiveSession: Boolean = false,
    val completedSessions: List<CompletedSession> = emptyList(),
    val requestedBuddyIds: Set<String> = emptySet(),
    val groupName: String = "",
    val groupVisibility: GroupVisibility = GroupVisibility.Private,
    val inviteText: String = "",
    val isGroupActionInProgress: Boolean = false,
    val invitableFriends: List<FriendProfile> = emptyList(),
    val invitedGroupFriendIds: Set<String> = emptySet(),
    val pendingGroupInvites: List<GroupInvite> = emptyList(),
    val error: String? = null,
    val newBadge: BadgeId? = null,
    val showReviewPrompt: Boolean = false,
    val pendingReviewSpotId: String? = null,
    val pendingReviewSpotName: String? = null,
    val isReviewSubmitting: Boolean = false,
    val noiseFilter: String? = null,
    val lightingFilter: String? = null,
    val wifiFilter: String? = null,
    val spaceTypeFilter: StudyMode? = null,
    val amenityFilter: String? = null,
    val occupancyFilter: Int? = null,
    val loadError: String? = null,
)

enum class HomeSection {
    Map,
    Explore,
    Social,
    Profile
}

enum class SocialTab {
    Friends,
    Requests,
    Discover
}

private const val MIN_GROUP_NAME_LENGTH = 2
private const val MAX_GROUP_NAME_LENGTH = 50

class HomeViewModel(
    private val repository: HomeRepository,
    private val authRepository: AuthRepository,
    private val streakRepository: StreakRepository,
    private val reviewRepository: ReviewRepository,
    private val awardBadgesUseCase: AwardBadgesUseCase,
    private val locationRepository: LocationRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pendingCheckoutBadge: BadgeId? = null
    private var groupObserverJob: Job? = null
    private var attendeeRefreshJob: Job? = null
    private var sharedLocationCountsRefreshJob: Job? = null
    private var currentObservedGroupId: String? = null

    init {
        observeOccupancy()
        observeSharedLocations()
        observePublicGroups()
        loadHome()
        subscribeGroupInvites()
    }

    fun selectMode(mode: StudyMode) {
        _uiState.update { it.copy(selectedMode = mode, selectedSection = HomeSection.Map, error = null) }
    }

    fun selectSection(section: HomeSection) {
        _uiState.update { state ->
            state.copy(
                selectedSection = section,
                selectedMode = if (section == HomeSection.Map) state.selectedMode else StudyMode.Solo,
                error = null
            )
        }
    }

    fun selectSocialTab(tab: SocialTab) {
        _uiState.update { it.copy(selectedSocialTab = tab, selectedSection = HomeSection.Social, error = null) }
    }

    fun selectMapSpot(id: String) {
        _uiState.update { it.copy(selectedSpotId = id, error = null) }
    }

    fun returnToSoloMap() {
        _uiState.update { it.copy(selectedMode = StudyMode.Solo, selectedSection = HomeSection.Map, error = null) }
    }

    fun startCheckIn(
        spot: StudySpotSummary,
        mode: StudyMode = uiState.value.selectedMode,
        onSuccess: () -> Unit = {}
    ) {
        val existing = uiState.value.activeCheckIn
        if (existing != null) {
            showError("You're already checked in to ${existing.spot.name}. End that session first.")
            return
        }
        val groupSessionId = if (mode == StudyMode.Group) {
            uiState.value.groupSession?.id ?: run {
                showError("Could not start a group session. Try again.")
                return
            }
        } else {
            null
        }
        viewModelScope.launch {
            runCatching {
                repository.startCheckIn(
                    spotId = spot.id,
                    mode = mode,
                    groupSessionId = groupSessionId,
                )
            }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            activeCheckIn = session,
                            sessionStartTimeMillis = System.currentTimeMillis(),
                            showLiveSession = true,
                            requestedBuddyIds = emptySet(),
                            error = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    showError(error.toUserMessage("Could not check in. Try again."))
                }
        }
    }

    fun minimizeSession() {
        _uiState.update { it.copy(showLiveSession = false, error = null) }
    }

    fun expandSession() {
        _uiState.update { it.copy(showLiveSession = true, error = null) }
    }

    fun closeCheckIn() {
        _uiState.update { it.copy(activeCheckIn = null, showLiveSession = false, error = null) }
    }

    fun checkOut(onCheckedOut: (CheckInSession) -> Unit = {}) {
        val session = uiState.value.activeCheckIn ?: return
        val elapsedSeconds = ((System.currentTimeMillis() - uiState.value.sessionStartTimeMillis) / 1000).toInt()
        val spotName = session.spot.name
        val spotId = session.spot.id
        viewModelScope.launch {
            runCatching { repository.checkOut(session.id) }
                .onSuccess {
                    val finished = CompletedSession(
                        spotName = spotName,
                        durationSeconds = elapsedSeconds,
                        finishedAtMillis = System.currentTimeMillis()
                    )
                    var earnedBadge: BadgeId? = null
                    val userId = runCatching { authRepository.currentUser()?.id }.getOrNull()
                    if (userId != null) {
                        runCatching {
                            val newCount = streakRepository.recordCheckout(userId, spotId, spotName, elapsedSeconds)
                            awardBadgesUseCase.onCheckout(userId, newCount)
                            earnedBadge = when (newCount) {
                                1  -> BadgeId.FIRST_CHECKOUT
                                10 -> BadgeId.SESSION_VETERAN
                                else -> null
                            }
                        }
                    }
                    pendingCheckoutBadge = earnedBadge
                    _uiState.update { state ->
                        state.copy(
                            activeCheckIn = null,
                            showLiveSession = false,
                            completedSessions = listOf(finished) + state.completedSessions,
                            showReviewPrompt = true,
                            pendingReviewSpotId = spotId,
                            pendingReviewSpotName = spotName,
                            error = null
                        )
                    }
                    onCheckedOut(session)
                }
                .onFailure { error ->
                    showError(error.toUserMessage("Could not check out. Try again."))
                }
        }
    }

    fun submitPostCheckoutReview(
        rating: Int,
        noiseLevel: String?,
        lighting: String?,
        wifiQuality: String?,
        occupancyPercent: Int?,
        comment: String?,
    ) {
        val spotId = uiState.value.pendingReviewSpotId ?: return
        if (rating !in 1..5 || uiState.value.isReviewSubmitting) return
        val checkoutBadge = pendingCheckoutBadge
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewSubmitting = true, error = null) }
            val qualityScore = ReviewQualityScorer.score(comment)
            val draft = ReviewDraft(
                spotSlug = spotId,
                rating = rating,
                noiseLevel = noiseLevel,
                lighting = lighting,
                wifiQuality = wifiQuality,
                occupancyPercent = occupancyPercent,
                comment = comment,
                qualityScore = qualityScore,
            )
            runCatching { reviewRepository.submit(draft) }
                .onSuccess {
                    var reviewBadge: BadgeId? = null
                    val userId = runCatching { authRepository.currentUser()?.id }.getOrNull()
                    if (userId != null) {
                        runCatching {
                            val reviewCount = reviewRepository.getReviewCount(userId)
                            awardBadgesUseCase.onReview(userId, qualityScore)
                            reviewBadge = when {
                                reviewCount == 1 -> BadgeId.FIRST_REVIEW
                                qualityScore >= 5 -> BadgeId.QUALITY_REVIEWER
                                else -> null
                            }
                        }
                    }
                    pendingCheckoutBadge = null
                    _uiState.update {
                        it.copy(
                            showReviewPrompt = false,
                            pendingReviewSpotId = null,
                            pendingReviewSpotName = null,
                            isReviewSubmitting = false,
                            newBadge = reviewBadge ?: checkoutBadge,
                        )
                    }
                    refresh()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            showReviewPrompt = false,
                            pendingReviewSpotId = null,
                            pendingReviewSpotName = null,
                            newBadge = checkoutBadge,
                            isReviewSubmitting = false,
                            error = throwable.toUserMessage("Could not post your review. Your draft is still here."),
                        )
                    }
                }
        }
    }

    fun dismissReviewPrompt() {
        val checkoutBadge = pendingCheckoutBadge
        pendingCheckoutBadge = null
        _uiState.update {
            it.copy(
                showReviewPrompt = false,
                pendingReviewSpotId = null,
                pendingReviewSpotName = null,
                isReviewSubmitting = false,
                newBadge = checkoutBadge,
            )
        }
    }

    fun clearNewBadge() {
        _uiState.update { it.copy(newBadge = null) }
    }

    fun sendBuddyRequest(studentId: String) {
        if (studentId in uiState.value.requestedBuddyIds) return
        viewModelScope.launch {
            runCatching { friendRepository.sendRequest(studentId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            requestedBuddyIds = state.requestedBuddyIds + studentId,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    showError(error.toUserMessage("Could not send buddy request."))
                }
        }
    }

    fun setNoiseFilter(filter: String?) {
        _uiState.update { it.copy(noiseFilter = filter, error = null) }
    }

    fun setLightingFilter(filter: String?) {
        _uiState.update { it.copy(lightingFilter = filter, error = null) }
    }

    fun setWifiFilter(filter: String?) {
        _uiState.update { it.copy(wifiFilter = filter, error = null) }
    }

    fun setSpaceTypeFilter(mode: StudyMode?) {
        _uiState.update { it.copy(spaceTypeFilter = mode, error = null) }
    }

    fun setAmenityFilter(amenity: String?) {
        _uiState.update { it.copy(amenityFilter = amenity, error = null) }
    }

    fun setOccupancyFilter(maxPercent: Int?) {
        _uiState.update { it.copy(occupancyFilter = maxPercent, error = null) }
    }

    fun updateUserLocation() {
        viewModelScope.launch {
            val latLng = runCatching { locationRepository.getLastLocation() }.getOrNull() ?: return@launch
            val (lat, lng) = latLng
            _uiState.update { state ->
                state.copy(
                    soloSpot = state.soloSpot?.withDistanceFrom(lat, lng),
                    groupSpots = state.groupSpots.map { it.withDistanceFrom(lat, lng) },
                    mapSpots = state.mapSpots.map { it.withDistanceFrom(lat, lng) },
                    error = null,
                )
            }
        }
    }

    private fun StudySpotSummary.withDistanceFrom(lat: Double, lng: Double): StudySpotSummary =
        if (latitude != null && longitude != null) {
            copy(distanceMeters = haversineMeters(lat, lng, latitude, longitude))
        } else this

    fun updateInviteText(value: String) {
        _uiState.update { it.copy(inviteText = value, error = null) }
    }

    fun updateGroupName(value: String) {
        if (value.length <= MAX_GROUP_NAME_LENGTH) {
            _uiState.update { it.copy(groupName = value, error = null) }
        }
    }

    fun selectGroupVisibility(visibility: GroupVisibility) {
        if (!uiState.value.isGroupActionInProgress) {
            _uiState.update { it.copy(groupVisibility = visibility, error = null) }
        }
    }

    fun createGroup() {
        val title = uiState.value.groupName.trim()
        val visibility = uiState.value.groupVisibility
        if (title.length < MIN_GROUP_NAME_LENGTH || uiState.value.isGroupActionInProgress) return
        if (uiState.value.groupSession != null) {
            showError("Leave your current group before creating another one.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGroupActionInProgress = true, error = null) }
            runCatching { repository.createGroup(title, visibility) }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            groupSession = session,
                            publicGroups = it.publicGroups.filterNot { group -> group.id == session.id },
                            groupName = "",
                            groupVisibility = GroupVisibility.Private,
                            isGroupActionInProgress = false,
                            error = null
                        )
                    }
                    startObservingGroupSession(session.id)
                    loadInvitableFriends()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGroupActionInProgress = false) }
                    showError(error.toUserMessage("Could not create your group. Try again."))
                }
        }
    }

    fun joinPublicGroup(groupSessionId: String) {
        if (uiState.value.groupSession != null || uiState.value.isGroupActionInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGroupActionInProgress = true, error = null) }
            runCatching { repository.joinPublicGroup(groupSessionId) }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            groupSession = session,
                            publicGroups = it.publicGroups.filterNot { group -> group.id == session.id },
                            isGroupActionInProgress = false,
                            error = null
                        )
                    }
                    startObservingGroupSession(session.id)
                    loadInvitableFriends()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGroupActionInProgress = false) }
                    showError(error.toUserMessage("Could not join this public group. Try again."))
                }
        }
    }

    fun leaveGroup() {
        val groupSession = uiState.value.groupSession ?: return
        if (uiState.value.isGroupActionInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGroupActionInProgress = true, error = null) }
            runCatching { repository.leaveGroup(groupSession.id) }
                .onSuccess {
                    stopObservingGroupSession()
                    _uiState.update { state ->
                        val publicGroups = if (
                            groupSession.visibility == GroupVisibility.Public && !groupSession.isOwner
                        ) {
                            (state.publicGroups + groupSession.copy(
                                members = emptyList(),
                                isOwner = false
                            )).distinctBy { it.id }
                        } else {
                            state.publicGroups.filterNot { it.id == groupSession.id }
                        }
                        state.copy(
                            groupSession = null,
                            publicGroups = publicGroups,
                            inviteText = "",
                            invitableFriends = emptyList(),
                            isGroupActionInProgress = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGroupActionInProgress = false) }
                    showError(error.toUserMessage("Could not leave the group. Try again."))
                }
        }
    }

    fun sendGroupInvite() {
        val inviteText = uiState.value.inviteText.trim()
        val groupSession = uiState.value.groupSession ?: return
        if (inviteText.isEmpty() || uiState.value.isGroupActionInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGroupActionInProgress = true, error = null) }
            runCatching { repository.inviteToGroup(groupSession.id, inviteText) }
                .onSuccess { result ->
                    when (result) {
                        is EmailInviteResult.Added -> {
                            _uiState.update { state ->
                                state.copy(
                                    groupSession = state.groupSession?.copy(
                                        members = (state.groupSession.members + result.member).distinctBy { it.id }
                                    ),
                                    inviteText = "",
                                    invitableFriends = state.invitableFriends.filterNot { it.id == result.member.id },
                                    isGroupActionInProgress = false,
                                    error = null
                                )
                            }
                        }
                        is EmailInviteResult.InviteSent -> {
                            _uiState.update { state ->
                                state.copy(
                                    inviteText = "",
                                    isGroupActionInProgress = false,
                                    error = null
                                )
                            }
                            showError("Invite sent to ${result.recipientName}. They'll see it on their group page.")
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGroupActionInProgress = false) }
                    showError(error.toUserMessage("Could not send invite."))
                }
        }
    }

    fun acceptGroupInvite(inviteId: String) {
        viewModelScope.launch {
            runCatching { repository.respondToGroupInvite(inviteId, accept = true) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(pendingGroupInvites = state.pendingGroupInvites.filterNot { it.id == inviteId })
                    }
                    // Reload home to pick up the new group session
                    runCatching { repository.loadHome() }
                        .onSuccess { snapshot -> applySnapshot(snapshot, isRefresh = true) }
                }
                .onFailure { error ->
                    showError(error.toUserMessage("Could not join the group. It may have ended."))
                    _uiState.update { state ->
                        state.copy(pendingGroupInvites = state.pendingGroupInvites.filterNot { it.id == inviteId })
                    }
                }
        }
    }

    fun declineGroupInvite(inviteId: String) {
        viewModelScope.launch {
            runCatching { repository.respondToGroupInvite(inviteId, accept = false) }
            _uiState.update { state ->
                state.copy(pendingGroupInvites = state.pendingGroupInvites.filterNot { it.id == inviteId })
            }
        }
    }

    private fun loadPendingGroupInvites() {
        viewModelScope.launch {
            val invites = runCatching { repository.fetchPendingGroupInvites() }.getOrDefault(emptyList())
            _uiState.update { it.copy(pendingGroupInvites = invites) }
        }
    }

    private fun subscribeGroupInvites() {
        viewModelScope.launch {
            val userId = runCatching { authRepository.currentUser()?.id }.getOrNull() ?: return@launch
            repository.observeGroupInvites(userId)
                .retryWhen { _, _ ->
                    delay(GROUP_INVITE_RECONNECT_DELAY_MILLIS)
                    true
                }
                .catch { /* best-effort */ }
                .collect { loadPendingGroupInvites() }
        }
    }

    fun inviteFriendToGroup(userId: String) {
        val groupSession = uiState.value.groupSession ?: return
        if (uiState.value.isGroupActionInProgress) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGroupActionInProgress = true, error = null) }
            runCatching { repository.inviteToGroupByUserId(groupSession.id, userId) }
                .onSuccess {
                    // Friend is now pending in their inbox
                    _uiState.update { state ->
                        state.copy(
                            invitedGroupFriendIds = state.invitedGroupFriendIds + userId,
                            isGroupActionInProgress = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isGroupActionInProgress = false) }
                    showError(error.toUserMessage("Could not invite this friend."))
                }
        }
    }

    fun refreshGroupMembers() {
        val groupId = uiState.value.groupSession?.id ?: return
        viewModelScope.launch {
            val members = runCatching { repository.fetchGroupSessionMembers(groupId) }.getOrNull()
                ?: return@launch
            _uiState.update { state ->
                state.copy(groupSession = state.groupSession?.copy(members = members))
            }
        }
    }

    fun loadInvitableFriends() {
        viewModelScope.launch {
            val friends = runCatching { friendRepository.fetchFriendProfiles() }.getOrDefault(emptyList())
            _uiState.update { it.copy(invitableFriends = friends.filter { f -> f.isAccepted }, invitedGroupFriendIds = emptySet()) }
        }
    }

    private fun startObservingGroupSession(groupSessionId: String) {
        if (groupSessionId == currentObservedGroupId) return
        groupObserverJob?.cancel()
        currentObservedGroupId = groupSessionId
        groupObserverJob = viewModelScope.launch {
            repository.observeGroupSession(groupSessionId)
                .retryWhen { _, _ ->
                    delay(GROUP_OBSERVER_RECONNECT_DELAY_MILLIS)
                    true
                }
                .catch { /* group updates are best-effort */ }
                .collect { event ->
                    when (event) {
                        is GroupSessionEvent.MembersChanged -> {
                            _uiState.update { state ->
                                state.copy(
                                    groupSession = state.groupSession?.copy(members = event.members)
                                )
                            }
                        }
                        GroupSessionEvent.Ended -> {
                            val wasOwner = _uiState.value.groupSession?.isOwner ?: false
                            stopObservingGroupSession()
                            _uiState.update { state ->
                                state.copy(
                                    groupSession = null,
                                    invitableFriends = emptyList(),
                                    inviteText = "",
                                    error = if (!wasOwner) "The group session has ended." else null,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun stopObservingGroupSession() {
        groupObserverJob?.cancel()
        groupObserverJob = null
        currentObservedGroupId = null
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadHome() }
                .onSuccess { snapshot ->
                    applySnapshot(snapshot, isRefresh = false)
                    launch {
                        val userId = runCatching { authRepository.currentUser()?.id }.getOrNull()
                        if (userId != null) {
                            val sessions = runCatching { streakRepository.fetchRecentSessions(userId) }.getOrDefault(emptyList())
                            if (sessions.isNotEmpty()) {
                                _uiState.update { state ->
                                    state.copy(completedSessions = sessions + state.completedSessions)
                                }
                            }
                        }
                    }
                    launch {
                        runCatching { repository.fetchActiveCheckIn() }
                            .getOrNull()
                            ?.let { (session, startMillis) ->
                                _uiState.update { state ->
                                    if (state.activeCheckIn == null) {
                                        state.copy(
                                            activeCheckIn = session,
                                            sessionStartTimeMillis = startMillis,
                                            showLiveSession = false,
                                        )
                                    } else state
                                }
                            }
                    }
                    launch { updateUserLocation() }
                    launch { loadPendingGroupInvites() }
                }
                .onFailure { error -> applyLoadFailure(error, isRefresh = false) }
        }
    }

    private fun applySnapshot(snapshot: HomeSnapshot, isRefresh: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                isRefreshing = false,
                loadError = null,
                userFirstName = snapshot.userFirstName,
                soloSpot = snapshot.soloSpot.withKnownOccupancy(state.occupancyBySpot),
                groupSession = snapshot.groupSession,
                groupSpots = snapshot.groupSpots.withKnownOccupancy(state.occupancyBySpot),
                publicGroups = snapshot.publicGroups,
                mapSpots = snapshot.mapSpots.withKnownOccupancy(state.occupancyBySpot),
                trendingCounts = snapshot.trendingCounts,
                // On refresh keep whatever spot the user was inspecting; on initial load default
                // to the featured solo spot.
                selectedSpotId = if (isRefresh) state.selectedSpotId else snapshot.soloSpot.id,
                error = null,
            )
        }
        snapshot.groupSession?.let { group ->
            startObservingGroupSession(group.id)
            if (!isRefresh) loadInvitableFriends()
        }
    }

    private fun applyLoadFailure(error: Throwable, isRefresh: Boolean) {
        val loadFallback = "We couldn't load live places. Check your connection and try again."
        val refreshFallback = "Could not refresh live places. Try again."
        _uiState.update { state ->
            when {
                !isRefresh || state.soloSpot == null -> state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    loadError = error.toUserMessage(loadFallback),
                    error = null,
                )
                else -> state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = error.toUserMessage(refreshFallback),
                )
            }
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return (r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toInt()
    }

    fun refresh() {
        if (uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            runCatching { repository.loadHome() }
                .onSuccess { snapshot ->
                    applySnapshot(snapshot, isRefresh = true)
                    launch { updateUserLocation() }
                }
                .onFailure { error -> applyLoadFailure(error, isRefresh = true) }
        }
    }

    fun retryLoad() {
        loadHome()
    }

    private fun observeOccupancy() {
        viewModelScope.launch {
            repository.observeOccupancy()
                .retryWhen { _, _ ->
                    delay(OCCUPANCY_RECONNECT_DELAY_MILLIS)
                    true
                }
                .catch { /* swallow: occupancy is best-effort, other state stays intact */ }
                .collect { occupancy ->
                    _uiState.update { state ->
                        state.copy(
                            soloSpot = state.soloSpot?.withOccupancy(occupancy),
                            groupSpots = state.groupSpots.map { it.withOccupancy(occupancy) },
                            mapSpots = state.mapSpots.map { it.withOccupancy(occupancy) },
                            occupancyBySpot = state.occupancyBySpot + (occupancy.spotId to occupancy),
                            activeCheckIn = state.activeCheckIn?.let { session ->
                                session.copy(spot = session.spot.withOccupancy(occupancy))
                            },
                            trendingCounts = occupancy.checkIns7d?.let { checkIns7d ->
                                state.trendingCounts + (occupancy.spotId to checkIns7d)
                            } ?: state.trendingCounts,
                        )
                    }
                    val activeSpotId = _uiState.value.activeCheckIn?.spot?.id
                    if (activeSpotId == occupancy.spotId) {
                        refreshCheckInAttendees(activeSpotId)
                    }
                }
        }
    }

    private fun refreshCheckInAttendees(spotSlug: String) {
        attendeeRefreshJob?.cancel()
        attendeeRefreshJob = viewModelScope.launch {
            val coAttendees = runCatching { repository.loadCheckInAttendees(spotSlug) }.getOrNull()
                ?: return@launch
            _uiState.update { state ->
                if (state.activeCheckIn?.spot?.id != spotSlug) return@update state
                val self = state.activeCheckIn?.attendees?.firstOrNull { it.isSelf }
                state.copy(
                    activeCheckIn = state.activeCheckIn?.copy(
                        attendees = listOfNotNull(self) + coAttendees
                    )
                )
            }
        }
    }

    private fun observeSharedLocations() {
        viewModelScope.launch {
            friendRepository.observeSharedLocations()
                .retryWhen { _, _ ->
                    delay(SHARED_LOCATIONS_RECONNECT_DELAY_MILLIS)
                    true
                }
                .catch { /* shared-location updates are best-effort */ }
                .collect {
                    // Privacy changes must update the open "Who's Here" list immediately.
                    // Keep this independent from the best-effort map count request so a slow
                    // or failed count cannot leave a newly hidden person visible.
                    _uiState.value.activeCheckIn?.spot?.id?.let(::refreshCheckInAttendees)
                    refreshSharedLocationCounts()
                }
        }
    }

    private fun refreshSharedLocationCounts() {
        sharedLocationCountsRefreshJob?.cancel()
        sharedLocationCountsRefreshJob = viewModelScope.launch {
            val counts = runCatching { repository.loadSharedLocationCounts() }.getOrNull()
                ?: return@launch
            _uiState.update { state ->
                state.copy(
                    soloSpot = state.soloSpot?.withSharedLocationCount(counts),
                    groupSpots = state.groupSpots.map { it.withSharedLocationCount(counts) },
                    mapSpots = state.mapSpots.map { it.withSharedLocationCount(counts) },
                )
            }
        }
    }

    private fun observePublicGroups() {
        viewModelScope.launch {
            repository.observePublicGroups()
                .retryWhen { _, _ ->
                    delay(PUBLIC_GROUPS_RECONNECT_DELAY_MILLIS)
                    true
                }
                .catch { /* public groups updates are best-effort */ }
                .collect {
                    val currentGroupId = _uiState.value.groupSession?.id
                    val fresh = runCatching {
                        repository.loadPublicGroupsSnapshot(excludingId = currentGroupId)
                    }.getOrNull()
                    if (fresh != null) {
                        _uiState.update { state -> state.copy(publicGroups = fresh) }
                    }
                }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val repository: HomeRepository,
        private val authRepository: AuthRepository,
        private val streakRepository: StreakRepository,
        private val reviewRepository: ReviewRepository,
        private val awardBadgesUseCase: AwardBadgesUseCase,
        private val locationRepository: LocationRepository,
        private val friendRepository: FriendRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(
                repository,
                authRepository,
                streakRepository,
                reviewRepository,
                awardBadgesUseCase,
                locationRepository,
                friendRepository,
            ) as T
    }
}

private fun StudySpotSummary.withSharedLocationCount(counts: Map<String, Int>): StudySpotSummary =
    copy(friendsHere = counts[id] ?: 0)

private fun StudySpotSummary.withKnownOccupancy(
    occupancies: Map<String, SpotOccupancy>
): StudySpotSummary =
    occupancies[id]?.let(::withOccupancy) ?: this

private fun List<StudySpotSummary>.withKnownOccupancy(
    occupancies: Map<String, SpotOccupancy>
): List<StudySpotSummary> =
    map { it.withKnownOccupancy(occupancies) }

private const val OCCUPANCY_RECONNECT_DELAY_MILLIS = 5_000L
private const val SHARED_LOCATIONS_RECONNECT_DELAY_MILLIS = 5_000L
private const val GROUP_OBSERVER_RECONNECT_DELAY_MILLIS = 5_000L
private const val PUBLIC_GROUPS_RECONNECT_DELAY_MILLIS = 5_000L
private const val GROUP_INVITE_RECONNECT_DELAY_MILLIS = 5_000L
