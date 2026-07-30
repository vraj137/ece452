package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.FriendProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface FriendRepository {
    suspend fun currentUserId(): String?
    suspend fun fetchFriendProfiles(): List<FriendProfile>
    suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile>
    suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile>
    suspend fun sendRequest(toUserId: String)
    suspend fun acceptRequest(friendshipId: String)
    suspend fun declineRequest(friendshipId: String)

    /**
     * Deletes the friendship row outright, which covers both unfriending an accepted friend and
     * cancelling a request you sent. Deleting (rather than marking declined) is what allows the
     * two users to connect again later.
     */
    suspend fun removeFriendship(friendshipId: String)

    suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile>

    fun observeFriendRequests(currentUserId: String): Flow<Unit> = emptyFlow()
}
