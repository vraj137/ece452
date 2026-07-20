package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import com.appetizers.spotra.domain.model.SocialSnapshot
import com.appetizers.spotra.domain.model.SocialUser
import com.appetizers.spotra.domain.repository.SocialRepository
import kotlinx.serialization.Serializable

class SupabaseAuthRepository(
    private val client: SupabaseClient
) : AuthRepository {
    override suspend fun currentUser(): AuthUser? =
        client.auth.currentUserOrNull()?.let { user ->
            val email = user.email
            if (email.isNullOrBlank()) null else AuthUser(user.id, email)
        }

    override suspend fun sendOtp(email: String, createUser: Boolean) {
        client.auth.signInWith(OTP) {
            this.email = email
            this.createUser = createUser
        }
    }

    override suspend fun verifyOtp(email: String, token: String): AuthUser {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
        return requireNotNull(currentUser()) { "Verification succeeded without a user session." }
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }
}

class SupabaseProfileRepository(
    private val client: SupabaseClient
) : ProfileRepository {
    override suspend fun getProfile(userId: String): UserProfile? =
        client.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfileDto>()
            ?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) {
        client.from("profiles").upsert(profile.toDto())
    }
}

@Serializable
private data class FriendRequestDto(
    val requester_id: String,
    val recipient_id: String,
    val status: String
)

class SupabaseSocialRepository(private val client: SupabaseClient) : SocialRepository {
    override suspend fun loadSocial(): SocialSnapshot {
        val userId = requireNotNull(client.auth.currentUserOrNull()?.id) { "Please sign in first." }
        val me = requireNotNull(client.from("profiles").select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfileDto>()) { "Complete your profile before using Social." }
        val peers = client.from("profiles").select {
            filter { eq("program", me.program); eq("study_term", me.studyTerm.label); neq("id", userId) }
        }.decodeList<UserProfileDto>().map { dto ->
            SocialUser(dto.userId, "${dto.firstName} ${dto.lastName}", dto.program, dto.studyTerm)
        }
        val sent = client.from("friend_requests").select {
            filter { eq("requester_id", userId) }
        }.decodeList<FriendRequestDto>()
        val received = client.from("friend_requests").select {
            filter { eq("recipient_id", userId) }
        }.decodeList<FriendRequestDto>()
        val byId = peers.associateBy { it.id }
        val acceptedIds = (sent.filter { it.status == "accepted" }.map { it.recipient_id } +
            received.filter { it.status == "accepted" }.map { it.requester_id }).toSet()
        val incomingIds = received.filter { it.status == "pending" }.map { it.requester_id }.toSet()
        val outgoingIds = sent.filter { it.status == "pending" }.map { it.recipient_id }.toSet()
        return SocialSnapshot(
            friends = acceptedIds.mapNotNull(byId::get),
            incomingRequests = incomingIds.mapNotNull(byId::get),
            suggestedUsers = peers.filter { it.id !in acceptedIds && it.id !in incomingIds && it.id !in outgoingIds },
            outgoingRequestIds = outgoingIds
        )
    }

    override suspend fun sendFriendRequest(recipientId: String) {
        val userId = requireNotNull(client.auth.currentUserOrNull()?.id) { "Please sign in first." }
        require(userId != recipientId) { "You cannot add yourself." }
        client.from("friend_requests").insert(FriendRequestDto(userId, recipientId, "pending"))
    }

    override suspend fun acceptFriendRequest(requesterId: String) {
        val userId = requireNotNull(client.auth.currentUserOrNull()?.id) { "Please sign in first." }
        client.from("friend_requests").update({ set("status", "accepted") }) {
            filter { eq("requester_id", requesterId); eq("recipient_id", userId); eq("status", "pending") }
        }
    }
}

class MissingConfigurationAuthRepository : AuthRepository {
    private fun missing(): Nothing = error(
        "Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to local.properties."
    )

    override suspend fun currentUser(): AuthUser? = null
    override suspend fun sendOtp(email: String, createUser: Boolean) = missing()
    override suspend fun verifyOtp(email: String, token: String): AuthUser = missing()
    override suspend fun signOut() = Unit
}

class MissingConfigurationProfileRepository : ProfileRepository {
    override suspend fun getProfile(userId: String): UserProfile? = null
    override suspend fun saveProfile(profile: UserProfile) {
        error("Supabase is not configured. Add credentials to local.properties.")
    }
}
