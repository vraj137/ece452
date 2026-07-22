package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.FriendshipStatus
import com.appetizers.spotra.domain.repository.FriendRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseFriendRepository(
    private val client: SupabaseClient
) : FriendRepository {

    override suspend fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    override suspend fun fetchFriendProfiles(): List<FriendProfile> {
        val userId = currentUserId() ?: return emptyList()

        val friendships = client.from("friendships")
            .select {
                filter {
                    or {
                        eq("requester_id", userId)
                        eq("addressee_id", userId)
                    }
                }
            }
            .decodeList<FriendshipRow>()

        if (friendships.isEmpty()) return emptyList()

        val otherIds = friendships.map { f ->
            if (f.requesterId == userId) f.addresseeId else f.requesterId
        }.distinct()

        val profiles = client.from("profiles")
            .select {
                filter { isIn("id", otherIds) }
            }
            .decodeList<FriendProfileRow>()

        val profileMap = profiles.associateBy { it.id }
        return friendships.mapNotNull { f ->
            val otherId = if (f.requesterId == userId) f.addresseeId else f.requesterId
            val profile = profileMap[otherId] ?: return@mapNotNull null
            FriendProfile(
                id = profile.id,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
                program = profile.program,
                term = profile.term,
                friendshipId = f.id,
                friendshipStatus = when (f.status) {
                    "accepted" -> FriendshipStatus.ACCEPTED
                    "declined" -> FriendshipStatus.DECLINED
                    else -> FriendshipStatus.PENDING
                },
                isRequester = f.requesterId == userId
            )
        }
    }

    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> {
        if (query.isBlank()) return emptyList()
        val selfId = currentUserId()
        return client.from("profiles")
            .select {
                filter {
                    or {
                        ilike("first_name", "%$query%")
                        ilike("last_name", "%$query%")
                        ilike("email", "%$query%")
                    }
                }
                limit(20)
            }
            .decodeList<FriendProfileRow>()
            .filter { it.id != selfId && it.id !in excludeIds }
            .map { FriendProfile(it.id, it.firstName, it.lastName, it.email, it.program, it.term) }
    }

    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> {
        val selfId = currentUserId() ?: return emptyList()

        if (acceptedFriendIds.isNotEmpty()) {
            val mutualIds = mutableSetOf<String>()
            coroutineScope {
                acceptedFriendIds.map { friendId ->
                    async {
                        runCatching {
                            client.from("friendships")
                                .select {
                                    filter {
                                        or {
                                            eq("requester_id", friendId)
                                            eq("addressee_id", friendId)
                                        }
                                        eq("status", "accepted")
                                    }
                                }
                                .decodeList<FriendshipRow>()
                        }.getOrDefault(emptyList())
                    }
                }.forEach { deferred ->
                    deferred.await().forEach { f ->
                        val otherId = if (f.requesterId in acceptedFriendIds) f.addresseeId else f.requesterId
                        if (otherId != selfId && otherId !in acceptedFriendIds) mutualIds.add(otherId)
                    }
                }
            }

            if (mutualIds.isNotEmpty()) {
                return client.from("profiles")
                    .select {
                        filter { isIn("id", mutualIds.toList()) }
                        limit(10)
                    }
                    .decodeList<FriendProfileRow>()
                    .map { FriendProfile(it.id, it.firstName, it.lastName, it.email, it.program, it.term) }
            }
        }

        // Fallback: suggest users in the same academic program
        val selfProfile = runCatching {
            client.from("profiles")
                .select { filter { eq("id", selfId) } }
                .decodeSingleOrNull<FriendProfileRow>()
        }.getOrNull() ?: return emptyList()

        if (selfProfile.program.isBlank()) return emptyList()

        return runCatching {
            client.from("profiles")
                .select {
                    filter {
                        ilike("program", selfProfile.program)
                        neq("id", selfId)
                    }
                    limit(10)
                }
                .decodeList<FriendProfileRow>()
                .filter { it.id !in acceptedFriendIds }
                .map { FriendProfile(it.id, it.firstName, it.lastName, it.email, it.program, it.term) }
        }.getOrDefault(emptyList())
    }

    override suspend fun sendRequest(toUserId: String) {
        val userId = currentUserId() ?: error("Not signed in.")
        client.from("friendships").insert(FriendshipInsert(requesterId = userId, addresseeId = toUserId))
    }

    override suspend fun acceptRequest(friendshipId: String) {
        client.from("friendships").update({ set("status", "accepted") }) {
            filter { eq("id", friendshipId) }
        }
    }

    override suspend fun declineRequest(friendshipId: String) {
        client.from("friendships").update({ set("status", "declined") }) {
            filter { eq("id", friendshipId) }
        }
    }

    override suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile> =
        runCatching {
            client.postgrest.rpc(
                "friends_at_spot",
                buildJsonObject { put("p_spot_slug", spotSlug) }
            )
                .decodeList<FriendAtSpotRow>()
                .map { FriendProfile(it.id, it.firstName, it.lastName) }
        }.getOrDefault(emptyList())

}

@Serializable
private data class FriendshipRow(
    val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String
)

@Serializable
private data class FriendshipInsert(
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String = "pending"
)

@Serializable
private data class FriendProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String = "",
    val program: String = "",
    val term: String = ""
)

@Serializable
private data class FriendAtSpotRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)
