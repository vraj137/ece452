package com.appetizers.spotra.data.remote

import com.appetizers.spotra.data.mock.MockData
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

// ── Spots / check-ins / home ────────────────────────────────────────────────

@Serializable
private data class SpotDto(
    val slug: String,
    val name: String,
    val building: String,
    val floor: String? = null,
    val latitude: Double,
    val longitude: Double,
    @SerialName("solo_friendly") val soloFriendly: Boolean = true,
    @SerialName("group_friendly") val groupFriendly: Boolean = true,
    val amenities: List<String> = emptyList()
)

@Serializable
private data class CheckInInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("spot_slug") val spotSlug: String,
    val mode: String,
    @SerialName("group_session_id") val groupSessionId: String? = null
)

@Serializable
private data class CheckInRow(
    val id: String,
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("ended_at") val endedAt: String? = null
)

// Aggregated occupancy from the spot_occupancy view — exposes only counts, never
// who is checked in (the underlying check_ins rows stay private to each user).
@Serializable
private data class OccupancyRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("active_count") val activeCount: Int
)

// 7-day check-in totals from the spot_trending view — counts only, drives the
// Explore "trending this week" ranking.
@Serializable
private data class TrendingRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("checkins_7d") val checkins7d: Int
)

class SupabaseHomeRepository(
    private val client: SupabaseClient
) : HomeRepository {
    // Group sessions and the buddy/invite social graph are not in the database
    // yet (Phase 4). Until then they are sourced from MockData so the group UI
    // keeps working; spots and check-ins below are fully real.
    private var groupSession = MockData.groupSession
    private var cachedFirstName: String = "there"
    private var spotCache: Map<String, StudySpotSummary> = emptyMap()

    override suspend fun loadHome(): HomeSnapshot = coroutineScope {
        val firstNameDeferred = async { currentFirstName() }
        val activeCountsDeferred = async { activeCheckInCounts() }
        val trendingDeferred = async { trendingCheckInCounts() }
        // Tolerate the spots table not being seeded/created yet — fall back to MockData
        // so the app still launches before the Phase 1 SQL has been applied.
        val dbSpotsDeferred = async {
            runCatching {
                client.from("spots").select().decodeList<SpotDto>()
            }.getOrDefault(emptyList())
        }

        cachedFirstName = firstNameDeferred.await()
        val activeCounts = activeCountsDeferred.await()
        val trendingCounts = trendingDeferred.await()
        val dbSpots = dbSpotsDeferred.await()

        // Fall back to MockData if the spots table hasn't been seeded yet, so the
        // app still runs before the Phase 1 SQL is applied.
        val summaries = if (dbSpots.isNotEmpty()) {
            dbSpots.map { it.toSummary(activeCounts[it.slug] ?: 0) }
        } else {
            MockData.spots.map { it.toSummary() }
        }
        spotCache = summaries.associateBy { it.id }

        val soloSpot = summaries.firstOrNull() ?: error("No study spots available.")
        val groupSpots = if (dbSpots.isNotEmpty()) {
            dbSpots.filter { it.groupFriendly }.map { it.toSummary(activeCounts[it.slug] ?: 0) }
        } else {
            MockData.groupSpots.map { it.toSummary() }
        }

        HomeSnapshot(
            userFirstName = cachedFirstName,
            soloSpot = soloSpot,
            groupSession = groupSession,
            groupSpots = groupSpots,
            mapSpots = summaries,
            trendingCounts = trendingCounts
        )
    }

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to check in.")
        val inserted = client.from("check_ins").insert(
            CheckInInsert(
                userId = userId,
                spotSlug = spotId,
                mode = if (mode == StudyMode.Group) "group" else "solo",
                groupSessionId = groupSessionId
            )
        ) { select() }.decodeSingle<CheckInRow>()

        val spot = spotCache[spotId]
            ?: MockData.spotById(spotId)?.toSummary()
            ?: error("Unknown study spot: $spotId")
        val self = CheckedInStudent(
            id = "you",
            initials = initialsOf(cachedFirstName),
            name = "You",
            detail = "Studying now",
            isSelf = true
        )
        return CheckInSession(id = inserted.id, spot = spot, mode = mode, attendees = listOf(self))
    }

    override suspend fun checkOut(sessionId: String) {
        client.from("check_ins").update({
            set("ended_at", Instant.now().toString())
        }) {
            filter { eq("id", sessionId) }
        }
    }

    // Phase 4 — social graph not in DB yet.
    override suspend fun sendBuddyRequest(studentId: String) = Unit

    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember {
        val initials = initialsOf(inviteText)
        return GroupMember(
            id = "invite-${inviteText.lowercase().replace(" ", "-")}-${groupSession.members.size}",
            name = inviteText,
            initials = initials
        ).also { member ->
            groupSession = groupSession.copy(members = groupSession.members + member)
        }
    }

    private suspend fun currentFirstName(): String {
        val userId = client.auth.currentUserOrNull()?.id ?: return "there"
        return client.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserProfileDto>()
            ?.firstName
            ?.takeIf { it.isNotBlank() }
            ?: "there"
    }

    // Active visits per spot, read from the privacy-preserving spot_occupancy view
    // (counts only). Resilient to the view not existing yet so the home screen
    // still loads before the migration is applied.
    private suspend fun activeCheckInCounts(): Map<String, Int> =
        runCatching {
            client.from("spot_occupancy").select().decodeList<OccupancyRow>()
                .associate { it.spotSlug to it.activeCount }
        }.getOrDefault(emptyMap())

    // 7-day check-in totals per spot from the spot_trending view (counts only).
    // Resilient to the view not existing yet — Explore just falls back to no ranking.
    private suspend fun trendingCheckInCounts(): Map<String, Int> =
        runCatching {
            client.from("spot_trending").select().decodeList<TrendingRow>()
                .associate { it.spotSlug to it.checkins7d }
        }.getOrDefault(emptyMap())

    private fun SpotDto.toSummary(activeCount: Int) = StudySpotSummary(
        id = slug,
        name = name,
        badge = occupancyBadge(activeCount),
        latitude = latitude,
        longitude = longitude
    )
}

private fun occupancyBadge(activeCount: Int): String = when {
    activeCount <= 0 -> "Quiet"
    activeCount < 8 -> "Moderate"
    else -> "Busy"
}

private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
        .take(2)

// ── Reviews ─────────────────────────────────────────────────────────────────

@Serializable
private data class ReviewInsert(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reviewer_name") val reviewerName: String?,
    val rating: Int,
    @SerialName("noise_level") val noiseLevel: String?,
    val lighting: String?,
    @SerialName("wifi_quality") val wifiQuality: String?,
    @SerialName("occupancy_percent") val occupancyPercent: Int?,
    val comment: String?,
    val anonymous: Boolean
)

@Serializable
private data class ReviewRow(
    val id: String,
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("reviewer_name") val reviewerName: String? = null,
    val rating: Int,
    @SerialName("noise_level") val noiseLevel: String? = null,
    val lighting: String? = null,
    @SerialName("wifi_quality") val wifiQuality: String? = null,
    @SerialName("occupancy_percent") val occupancyPercent: Int? = null,
    val comment: String? = null,
    val anonymous: Boolean = false
) {
    fun toReview(): Review {
        val displayName = if (anonymous || reviewerName.isNullOrBlank()) "Anonymous" else reviewerName
        return Review(
            id = id,
            spotSlug = spotSlug,
            reviewerName = displayName,
            reviewerInitials = initialsOf(displayName),
            reviewerId = if (anonymous) "anon" else (userId ?: "anon"),
            rating = rating,
            noiseLevel = noiseLevel,
            lighting = lighting,
            wifiQuality = wifiQuality,
            occupancyPercent = occupancyPercent,
            comment = comment,
            anonymous = anonymous
        )
    }
}

class SupabaseReviewRepository(
    private val client: SupabaseClient
) : ReviewRepository {
    override suspend fun reviewsFor(spotSlug: String): List<Review> =
        client.from("reviews")
            .select {
                filter { eq("spot_slug", spotSlug) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ReviewRow>()
            .map { it.toReview() }

    override suspend fun submit(draft: ReviewDraft) {
        val user = client.auth.currentUserOrNull()
            ?: error("You need to be signed in to review.")
        val displayName = if (draft.anonymous) {
            "Anonymous"
        } else {
            client.from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<UserProfileDto>()
                ?.firstName
                ?.takeIf { it.isNotBlank() }
                ?: "Student"
        }
        client.from("reviews").insert(
            ReviewInsert(
                spotSlug = draft.spotSlug,
                userId = user.id,
                reviewerName = displayName,
                rating = draft.rating,
                noiseLevel = draft.noiseLevel,
                lighting = draft.lighting,
                wifiQuality = draft.wifiQuality,
                occupancyPercent = draft.occupancyPercent,
                comment = draft.comment,
                anonymous = draft.anonymous
            )
        )
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
