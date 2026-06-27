package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from

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
private data class SpotSubmissionDto(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val building: String,
    val floor: String,
    @SerialName("submitted_by_email") val submittedByEmail: String,
    @SerialName("submitted_by_user_id") val submittedByUserId: String?,
    val status: String = "pending"
)

class SupabaseSpotSubmissionRepository(
    private val client: SupabaseClient
) : SpotSubmissionRepository {
    override suspend fun submitSpot(submission: SpotSubmission) {
        client.from("user_spot_submissions").insert(
            SpotSubmissionDto(
                name = submission.name,
                description = submission.description,
                latitude = submission.latitude,
                longitude = submission.longitude,
                building = submission.building,
                floor = submission.floor,
                submittedByEmail = submission.submittedByEmail,
                submittedByUserId = submission.submittedByUserId
            )
        )
    }
}

class DebugSpotSubmissionRepository : SpotSubmissionRepository {
    override suspend fun submitSpot(submission: SpotSubmission) = Unit
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
