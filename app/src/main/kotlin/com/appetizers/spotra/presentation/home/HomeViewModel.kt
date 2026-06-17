package com.appetizers.spotra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.model.CheckInSession
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
    val userFirstName: String = "",
    val selectedMode: StudyMode = StudyMode.Solo,
    val soloSpot: StudySpotSummary? = null,
    val groupSession: GroupStudySession? = null,
    val groupSpots: List<StudySpotSummary> = emptyList(),
    val mapSpots: List<StudySpotSummary> = emptyList(),
    val selectedSpotId: String? = null,
    val activeCheckIn: CheckInSession? = null,
    val requestedBuddyIds: Set<String> = emptySet(),
    val inviteText: String = "",
    val error: String? = null
)

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun selectMode(mode: StudyMode) {
        _uiState.update { it.copy(selectedMode = mode, error = null) }
    }

    fun selectMapSpot(id: String) {
        _uiState.update { it.copy(selectedSpotId = id, error = null) }
    }

    fun returnToSoloMap() {
        _uiState.update { it.copy(selectedMode = StudyMode.Solo, error = null) }
    }

    fun startCheckIn(spot: StudySpotSummary, mode: StudyMode = uiState.value.selectedMode) {
        val groupSession = uiState.value.groupSession ?: return
        viewModelScope.launch {
            runCatching { repository.startCheckIn(spot.id, mode, groupSession.id) }
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            activeCheckIn = session,
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

    fun closeCheckIn() {
        _uiState.update { it.copy(activeCheckIn = null, error = null) }
    }

    fun checkOut() {
        val session = uiState.value.activeCheckIn ?: return
        viewModelScope.launch {
            runCatching { repository.checkOut(session.id) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(activeCheckIn = null, error = null)
                    }
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
