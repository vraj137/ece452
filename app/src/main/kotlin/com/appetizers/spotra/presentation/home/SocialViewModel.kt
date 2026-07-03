package com.appetizers.spotra.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SocialViewModel(
    private val friendRepository: FriendRepository
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val friends: List<FriendProfile> = emptyList(),
        val incomingRequests: List<FriendProfile> = emptyList(),
        val outgoingRequests: List<FriendProfile> = emptyList(),
        val suggestions: List<FriendProfile> = emptyList(),
        val searchQuery: String = "",
        val searchResults: List<FriendProfile> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val all = friendRepository.fetchFriendProfiles()
                val accepted = all.filter { it.isAccepted }
                val incoming = all.filter { it.isPendingIncoming }
                val outgoing = all.filter { it.isPendingOutgoing }
                val acceptedIds = accepted.map { it.id }.toSet()
                val suggested = friendRepository.fetchSuggested(acceptedIds)
                _state.update {
                    it.copy(
                        isLoading = false,
                        friends = accepted,
                        incomingRequests = incoming,
                        outgoingRequests = outgoing,
                        suggestions = suggested,
                        error = null
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            runCatching {
                val current = _state.value
                val excludeIds = (current.friends + current.incomingRequests + current.outgoingRequests)
                    .map { it.id }.toSet()
                val selfId = friendRepository.currentUserId()
                friendRepository.searchUsers(query, excludeIds + setOfNotNull(selfId))
            }.onSuccess { results ->
                _state.update { it.copy(searchResults = results) }
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun sendRequest(profile: FriendProfile) {
        viewModelScope.launch {
            runCatching { friendRepository.sendRequest(profile.id) }
                .onSuccess {
                    _state.update { state ->
                        val pending = profile.copy(friendshipStatus = com.appetizers.spotra.domain.model.FriendshipStatus.PENDING, isRequester = true)
                        state.copy(
                            searchResults = state.searchResults.filter { it.id != profile.id },
                            suggestions = state.suggestions.filter { it.id != profile.id },
                            outgoingRequests = state.outgoingRequests + pending,
                            error = null
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun acceptRequest(profile: FriendProfile) {
        val friendshipId = profile.friendshipId ?: return
        viewModelScope.launch {
            runCatching { friendRepository.acceptRequest(friendshipId) }
                .onSuccess {
                    _state.update { state ->
                        val accepted = profile.copy(friendshipStatus = com.appetizers.spotra.domain.model.FriendshipStatus.ACCEPTED)
                        state.copy(
                            incomingRequests = state.incomingRequests.filter { it.id != profile.id },
                            friends = state.friends + accepted,
                            error = null
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun declineRequest(profile: FriendProfile) {
        val friendshipId = profile.friendshipId ?: return
        viewModelScope.launch {
            runCatching { friendRepository.declineRequest(friendshipId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            incomingRequests = state.incomingRequests.filter { it.id != profile.id },
                            error = null
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    class Factory(
        private val friendRepository: FriendRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SocialViewModel(friendRepository) as T
    }
}
