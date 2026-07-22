package com.appetizers.spotra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.data.location.LocationRepository
import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import com.appetizers.spotra.domain.usecase.ReviewQualityScorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val mapSpots: List<StudySpotSummary> = emptyList(),
    val trendingCounts: Map<String, Int> = emptyMap(),
    val selectedSpotId: String? = null,
    val activeCheckIn: CheckInSession? = null,
    val sessionStartTimeMillis: Long = 0L,
    val showLiveSession: Boolean = false,
    val completedSessions: List<CompletedSession> = emptyList(),
    val requestedBuddyIds: Set<String> = emptySet(),
    val inviteText: String = "",
    val error: String? = null,
    val newBadge: BadgeId? = null,
    val pendingCheckoutBadge: BadgeId? = null,
    val showReviewPrompt: Boolean = false,
    val pendingReviewSpotId: String? = null,
    val pendingReviewSpotName: String? = null,
    val noiseFilter: String? = null,
    val spaceTypeFilter: StudyMode? = null,
    val amenityFilter: String? = null,
    val occupancyFilter: Int? = null,
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

    init {
        loadHome()
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
        val groupSessionId = if (mode == StudyMode.Group) {
            uiState.value.groupSession?.id ?: run {
                showError("Could not start a group session. Try again.")
                return
            }
        } else {
            null
        }
        viewModelScope.launch {
            runCatching { repository.startCheckIn(spot.id, mode, groupSessionId) }
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
                    showError(error.message ?: "Could not check in. Try again.")
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
                    _uiState.update { state ->
                        state.copy(
                            activeCheckIn = null,
                            showLiveSession = false,
                            completedSessions = listOf(finished) + state.completedSessions,
                            pendingCheckoutBadge = earnedBadge,
                            showReviewPrompt = true,
                            pendingReviewSpotId = spotId,
                            pendingReviewSpotName = spotName,
                            error = null
                        )
                    }
                    onCheckedOut(session)
                }
                .onFailure { error ->
                    showError(error.message ?: "Could not check out. Try again.")
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
        val checkoutBadge = uiState.value.pendingCheckoutBadge
        viewModelScope.launch {
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
                    _uiState.update {
                        it.copy(
                            showReviewPrompt = false,
                            pendingReviewSpotId = null,
                            pendingReviewSpotName = null,
                            pendingCheckoutBadge = null,
                            newBadge = reviewBadge ?: checkoutBadge,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            showReviewPrompt = false,
                            pendingReviewSpotId = null,
                            pendingReviewSpotName = null,
                            pendingCheckoutBadge = null,
                            newBadge = checkoutBadge,
                        )
                    }
                }
        }
    }

    fun dismissReviewPrompt() {
        val checkoutBadge = uiState.value.pendingCheckoutBadge
        _uiState.update {
            it.copy(
                showReviewPrompt = false,
                pendingReviewSpotId = null,
                pendingReviewSpotName = null,
                pendingCheckoutBadge = null,
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
                    showError(error.message ?: "Could not send buddy request.")
                }
        }
    }

    fun setNoiseFilter(filter: String?) {
        _uiState.update { it.copy(noiseFilter = filter, error = null) }
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
            val latLng = runCatching { locationRepository.getLastLocation() }.getOrNull()
            if (latLng != null) {
                val updatedSpots = _uiState.value.mapSpots.map { spot ->
                    if (spot.latitude != null && spot.longitude != null) {
                        spot.copy(distanceMeters = haversineMeters(latLng.first, latLng.second, spot.latitude, spot.longitude))
                    } else spot
                }
                _uiState.update { it.copy(mapSpots = updatedSpots, error = null) }
            }
        }
    }

    fun updateInviteText(value: String) {
        _uiState.update { it.copy(inviteText = value, error = null) }
    }

    fun sendGroupInvite() {
        val inviteText = uiState.value.inviteText.trim()
        val groupSession = uiState.value.groupSession ?: return
        if (inviteText.isEmpty()) return

        viewModelScope.launch {
            runCatching { repository.inviteToGroup(groupSession.id, inviteText) }
                .onSuccess { member ->
                    _uiState.update { state ->
                        state.copy(
                            groupSession = groupSession.copy(
                                members = groupSession.members + member
                            ),
                            inviteText = "",
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    showError(error.message ?: "Could not send invite.")
                }
        }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadHome() }
                .onSuccess { snapshot ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userFirstName = snapshot.userFirstName,
                            soloSpot = snapshot.soloSpot,
                            groupSession = snapshot.groupSession,
                            groupSpots = snapshot.groupSpots,
                            mapSpots = snapshot.mapSpots,
                            trendingCounts = snapshot.trendingCounts,
                            selectedSpotId = snapshot.soloSpot.id,
                            error = null
                        )
                    }
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
                    launch { updateUserLocation() }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Could not load study spots."
                        )
                    }
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
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            userFirstName = snapshot.userFirstName,
                            soloSpot = snapshot.soloSpot,
                            groupSession = snapshot.groupSession,
                            groupSpots = snapshot.groupSpots,
                            mapSpots = snapshot.mapSpots,
                            trendingCounts = snapshot.trendingCounts,
                            error = null
                        )
                    }
                    launch { updateUserLocation() }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Could not refresh spots."
                        )
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
