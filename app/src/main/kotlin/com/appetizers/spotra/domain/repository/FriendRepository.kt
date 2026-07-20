package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.FriendProfile

interface FriendRepository {
    suspend fun currentUserId(): String?
    // All friend profiles for the current user (all statuses), caller filters.
    suspend fun fetchFriendProfiles(): List<FriendProfile>
    suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile>
    suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile>
    suspend fun sendRequest(toUserId: String)
    suspend fun acceptRequest(friendshipId: String)
    suspend fun declineRequest(friendshipId: String)
    suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile>
}
