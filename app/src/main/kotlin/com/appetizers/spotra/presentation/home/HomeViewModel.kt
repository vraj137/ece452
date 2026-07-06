package com.appetizers.spotra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.HomeRepository
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
    val error: String? = null
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
    private val repository: HomeRepository
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

    fun startCheckIn(spot: StudySpotSummary, mode: StudyMode = uiState.value.selectedMode) {
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
        viewModelScope.launch {
            runCatching { repository.checkOut(session.id) }
                .onSuccess {
                    val finished = CompletedSession(
                        spotName = spotName,
                        durationSeconds = elapsedSeconds,
                        finishedAtMillis = System.currentTimeMillis()
                    )
                    _uiState.update { state ->
                        state.copy(
                            activeCheckIn = null,
                            showLiveSession = false,
                            completedSessions = listOf(finished) + state.completedSessions,
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

    fun sendBuddyRequest(studentId: String) {
        if (studentId in uiState.value.requestedBuddyIds) return
        viewModelScope.launch {
            runCatching { repository.sendBuddyRequest(studentId) }
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

    // Re-pull live occupancy (and spot list) without the full-screen loading state.
    // Unlike loadHome() this preserves the user's current map selection.
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

    class Factory(
        private val repository: HomeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
