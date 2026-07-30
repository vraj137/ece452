package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.FriendshipStatus
import com.appetizers.spotra.domain.repository.FriendRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
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

        val (profiles, streaks) = coroutineScope {
            val profilesDeferred = async { safeProfiles(ids = otherIds) }
            val streaksDeferred = async {
                runCatching {
                    client.postgrest.rpc("friend_streaks")
                        .decodeList<FriendStreakRow>()
                        .associate { it.userId to it.loginStreak }
                }.getOrDefault(emptyMap())
            }
            profilesDeferred.await() to streaksDeferred.await()
        }

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
                isRequester = f.requesterId == userId,
                streakDays = streaks[profile.id]?.takeIf { f.status == "accepted" },
            )
        }
    }

    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> {
        if (query.isBlank()) return emptyList()
        val selfId = currentUserId()
        return safeProfiles(query = query)
            .filter { it.id != selfId && it.id !in excludeIds }
            .map { FriendProfile(it.id, it.firstName, it.lastName, it.email, it.program, it.term) }
    }

    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> {
        return runCatching {
            client.postgrest.rpc(
                "discover_profiles",
                buildJsonObject { put("p_limit", 20) }
            )
                .decodeList<ProfileRow>()
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

    override suspend fun removeFriendship(friendshipId: String) {
        client.postgrest.rpc(
            "remove_friendship",
            buildJsonObject { put("p_friendship_id", friendshipId) }
        )
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

    override fun observeFriendRequests(currentUserId: String): Flow<Unit> = channelFlow {
        val realtimeChannel = client.channel("friend-requests-$currentUserId") { isPrivate = true }
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            realtimeChannel
                .broadcastFlow<FriendRequestBroadcast>("friend_request_received")
                .collect { send(Unit) }
        }
        try {
            withTimeout(FRIEND_REQUEST_SUBSCRIBE_TIMEOUT_MILLIS) {
                realtimeChannel.subscribe(blockUntilSubscribed = true)
            }
            awaitCancellation()
        } finally {
            collector.cancelAndJoin()
            client.realtime.removeChannel(realtimeChannel)
        }
    }.buffer(Channel.CONFLATED)

    private suspend fun safeProfiles(
        ids: List<String>? = null,
        query: String? = null,
        program: String? = null,
        limit: Int = 20,
    ): List<ProfileRow> =
        client.postgrest.rpc(
            "safe_profiles",
            buildJsonObject {
                ids?.let {
                    put("p_ids", JsonArray(it.map(::JsonPrimitive)))
                }
                query?.let { put("p_query", it.trim()) }
                program?.let { put("p_program", it) }
                put("p_limit", limit)
            }
        ).decodeList()
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
internal data class ProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String = "",
    val program: String = "",
    @SerialName("study_term") val term: String = ""
)

@Serializable
private data class FriendAtSpotRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)

@Serializable
private data class FriendStreakRow(
    @SerialName("user_id") val userId: String,
    @SerialName("login_streak") val loginStreak: Int,
)

@Serializable
private data class FriendRequestBroadcast(
    @SerialName("from_user_id") val fromUserId: String,
    val status: String = "pending"
)

private const val FRIEND_REQUEST_SUBSCRIBE_TIMEOUT_MILLIS = 15_000L
