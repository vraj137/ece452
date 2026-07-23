package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.DailyOperatingHours
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SocialSnapshot
import com.appetizers.spotra.domain.model.SocialUser
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.model.WATERLOO_TIME_ZONE
import com.appetizers.spotra.domain.model.WeeklyOperatingHours
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.SocialRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

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
        val studyTerm = me.studyTerm ?: return SocialSnapshot()
        val peers = client.from("profiles").select {
            filter {
                eq("program", me.program)
                eq("study_term", studyTerm.label)
                neq("id", userId)
            }
        }.decodeList<UserProfileDto>().mapNotNull { dto ->
            dto.studyTerm?.let { term ->
                SocialUser(dto.userId, "${dto.firstName} ${dto.lastName}", dto.program, term)
            }
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
    @SerialName("booking_url") val bookingUrl: String? = null,
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
                submittedByUserId = submission.submittedByUserId,
                bookingUrl = submission.bookingUrl?.takeIf { it.isNotBlank() },
            )
        )
    }
}

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
    val amenities: List<String> = emptyList(),
    val capacity: Int? = null,
    @SerialName("parent_slug") val parentSlug: String? = null,
    @SerialName("booking_url") val bookingUrl: String? = null,
    @SerialName("noise_level") val noiseLevel: String? = null,
    val lighting: String? = null,
    @SerialName("hours_of_operation") val hoursOfOperation: JsonObject? = null,
    @SerialName("time_zone") val timeZone: String = WATERLOO_TIME_ZONE,
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

@Serializable
private data class GroupSessionDto(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("ended_at") val endedAt: String? = null,
)

@Serializable
private data class GroupSessionInsert(
    val title: String,
    val subtitle: String? = null,
    @SerialName("created_by") val createdBy: String,
)

@Serializable
private data class GroupSessionMemberInsert(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
)

@Serializable
private data class GroupSessionMemberRow(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
)

@Serializable
private data class OccupancyRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("active_count") val activeCount: Int
)

@Serializable
private data class TrendingRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("checkins_7d") val checkins7d: Int
)

@Serializable
private data class ActiveCheckInRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("ended_at") val endedAt: String? = null,
)

@Serializable
private data class AttendeeProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val program: String = "",
)

@Serializable
private data class FriendshipReadRow(
    val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String,
)

@Serializable
private data class FriendsAllSpotsRow(
    @SerialName("spot_slug") val spotSlug: String,
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
)

class SupabaseHomeRepository(
    private val client: SupabaseClient
) : HomeRepository {
    private var activeGroupSessionId: String? = null
    private var cachedFirstName: String = "there"
    private var spotCache: Map<String, StudySpotSummary> = emptyMap()

    override suspend fun loadHome(): HomeSnapshot = coroutineScope {
        val firstNameDeferred = async { currentFirstName() }
        val activeCountsDeferred = async { activeCheckInCounts() }
        val trendingDeferred = async {
            runCatching { trendingCheckInCounts() }.getOrDefault(emptyMap())
        }
        val dbSpotsDeferred = async {
            client.from("spots").select().decodeList<SpotDto>()
        }
        val groupSessionDeferred = async {
            runCatching { loadOrCreateGroupSession() }.getOrNull()
        }
        val friendsAtSpotsDeferred = async {
            runCatching {
                client.postgrest.rpc("friends_all_spots")
                    .decodeList<FriendsAllSpotsRow>()
                    .groupBy { it.spotSlug }
                    .mapValues { (_, rows) -> rows.size }
            }.getOrDefault(emptyMap())
        }

        cachedFirstName = firstNameDeferred.await()
        val activeCounts = activeCountsDeferred.await()
        val trendingCounts = trendingDeferred.await()
        val dbSpots = dbSpotsDeferred.await()
        val childCounts = dbSpots.childCountsByParentSlug()
        val parentSpots = dbSpots.filter { it.parentSlug == null }

        val baseSummaries = parentSpots.map { spot ->
            spot.toSummary(
                activeCount = activeCounts[spot.slug] ?: 0,
                childCount = childCounts[spot.slug] ?: 0
            )
        }

        val friendsPerSpot = friendsAtSpotsDeferred.await()
        val summaries = baseSummaries.map { s ->
            val count = friendsPerSpot[s.id] ?: 0
            if (count > 0) s.copy(friendsHere = count) else s
        }
        spotCache = summaries.associateBy { it.id }

        val soloSpot = summaries.firstOrNull()
            ?: error("No live study spots are available in Supabase yet.")
        val groupSpots = summaries.filter { it.groupFriendly }

        val loadedGroupSession = groupSessionDeferred.await()

        HomeSnapshot(
            userFirstName = cachedFirstName,
            soloSpot = soloSpot,
            groupSession = loadedGroupSession,
            groupSpots = groupSpots,
            mapSpots = summaries,
            trendingCounts = trendingCounts,
        )
    }

    override suspend fun spotDetail(spotId: String): StudySpotDetail = coroutineScope {
        val spotDeferred = async {
            client.from("spots")
                .select { filter { eq("slug", spotId) } }
                .decodeSingleOrNull<SpotDto>()
        }
        val activeCountsDeferred = async { activeCheckInCounts() }
        val reviewsDeferred = async { reviewRowsForSpot(spotId) }

        val activeCount = activeCountsDeferred.await()[spotId] ?: 0
        val reviews = reviewsDeferred.await()
        val dbSpot = spotDeferred.await()

        requireNotNull(dbSpot) { "Study spot not found in Supabase: $spotId" }
            .toDetail(activeCount, reviews)
    }

    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = coroutineScope {
        val childrenDeferred = async {
            client.from("spots")
                .select { filter { eq("parent_slug", parentSpotId) } }
                .decodeList<SpotDto>()
        }
        val activeCountsDeferred = async { activeCheckInCounts() }

        val children = childrenDeferred.await()
        val activeCounts = activeCountsDeferred.await()

        children
            .sortedWith(compareBy<SpotDto> { it.floor.orEmpty() }.thenBy { it.name })
            .map { spot ->
                spot.toDetail(
                    activeCount = activeCounts[spot.slug] ?: 0,
                    reviews = emptyList()
                )
            }
    }

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to check in.")
        client.from("check_ins").update({
            set("ended_at", Instant.now().toString())
        }) {
            filter {
                eq("user_id", userId)
                exact("ended_at", null)
            }
        }
        val inserted = client.from("check_ins").insert(
            CheckInInsert(
                userId = userId,
                spotSlug = spotId,
                mode = if (mode == StudyMode.Group) "group" else "solo",
                groupSessionId = groupSessionId
            )
        ) { select() }.decodeSingle<CheckInRow>()

        val spot = spotCache[spotId]
            ?: dbSpotSummary(spotId)
            ?: error("Study spot not found in Supabase: $spotId")
        val self = CheckedInStudent(
            id = "you",
            initials = initialsOf(cachedFirstName),
            name = "You",
            detail = "Studying now",
            isSelf = true
        )
        val coAttendees = loadCoAttendees(userId, spotId)
        return CheckInSession(id = inserted.id, spot = spot, mode = mode, attendees = listOf(self) + coAttendees)
    }

    private suspend fun loadCoAttendees(selfId: String, spotId: String): List<CheckedInStudent> = coroutineScope {
        val active = client.from("check_ins").select {
            filter {
                eq("spot_slug", spotId)
                exact("ended_at", null)
            }
        }.decodeList<ActiveCheckInRow>().filter { it.userId != selfId }

        if (active.isEmpty()) return@coroutineScope emptyList()

        val otherIds = active.map { it.userId }.distinct()

        val profilesDeferred = async {
            client.from("profiles").select {
                filter { isIn("id", otherIds) }
            }.decodeList<AttendeeProfileRow>().associateBy { it.id }
        }
        val friendshipsDeferred = async {
            client.from("friendships").select {
                filter { or { eq("requester_id", selfId); eq("addressee_id", selfId) } }
            }.decodeList<FriendshipReadRow>().filter { f ->
                (f.requesterId == selfId && f.addresseeId in otherIds) ||
                (f.addresseeId == selfId && f.requesterId in otherIds)
            }
        }

        val profiles = profilesDeferred.await()
        val friendships = friendshipsDeferred.await()

        val friendIds = friendships.filter { it.status == "accepted" }
            .flatMap { listOf(it.requesterId, it.addresseeId) }
            .filter { it != selfId }.toSet()
        val sentMeIds = friendships
            .filter { it.status == "pending" && it.addresseeId == selfId }
            .map { it.requesterId }.toSet()

        otherIds.mapNotNull { id ->
            val p = profiles[id] ?: return@mapNotNull null
            CheckedInStudent(
                id = id,
                initials = buildString {
                    p.firstName.firstOrNull()?.uppercaseChar()?.let { append(it) }
                    p.lastName.firstOrNull()?.uppercaseChar()?.let { append(it) }
                }.ifBlank { "?" },
                name = "${p.firstName} ${p.lastName.firstOrNull() ?: ""}.",
                detail = p.program.ifBlank { "Studying here" },
                isFriend = id in friendIds,
                hasSentMeRequest = id in sentMeIds,
            )
        }
    }

    override suspend fun checkOut(sessionId: String) {
        client.from("check_ins").update({
            set("ended_at", Instant.now().toString())
        }) {
            filter { eq("id", sessionId) }
        }
    }

    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember {
        require(groupSessionId.isNotBlank()) { "A live group session is required before inviting members." }
        require(activeGroupSessionId == null || activeGroupSessionId == groupSessionId) {
            "This group session is no longer active. Refresh and try again."
        }

        val profile = if (inviteText.contains("@")) {
            client.from("profiles")
                .select { filter { eq("email", inviteText.trim()) } }
                .decodeSingleOrNull<UserProfileDto>()
        } else null

        return if (profile != null) {
            client.from("group_session_members").insert(
                GroupSessionMemberInsert(sessionId = groupSessionId, userId = profile.userId)
            )
            val initials = buildString {
                append(profile.firstName.firstOrNull()?.uppercaseChar() ?: '?')
                append(profile.lastName.firstOrNull()?.uppercaseChar() ?: '?')
            }
            val displayName = "${profile.firstName} ${profile.lastName.first()}."
            GroupMember(id = profile.userId, name = displayName, initials = initials)
        } else if (!inviteText.contains("@")) {
            error("Enter the email address associated with their Spotra account to invite them.")
        } else {
            error("No Spotra account found for that email. Check the address and try again.")
        }
    }

    private suspend fun loadOrCreateGroupSession(): GroupStudySession = coroutineScope {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to use group mode.")

        val existingSession = client.from("group_sessions")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<GroupSessionDto>()
            .firstOrNull { it.endedAt == null }

        val session = existingSession ?: run {
            val created = client.from("group_sessions").insert(
                GroupSessionInsert(
                    title = "Study sesh",
                    subtitle = "Let's find a spot!",
                    createdBy = userId
                )
            ) { select() }.decodeSingle<GroupSessionDto>()
            client.from("group_session_members").insert(
                GroupSessionMemberInsert(
                    sessionId = created.id,
                    userId = userId,
                    role = "owner"
                )
            )
            created
        }
        activeGroupSessionId = session.id

        val memberUserIds = client.from("group_session_members")
            .select { filter { eq("session_id", session.id) } }
            .decodeList<GroupSessionMemberRow>()
            .map { it.userId }

        val profileDeferreds = memberUserIds.map { memberId ->
            async {
                client.from("profiles")
                    .select { filter { eq("id", memberId) } }
                    .decodeSingleOrNull<UserProfileDto>()
            }
        }
        val profiles = profileDeferreds.map { it.await() }.filterNotNull()

        val members = profiles.map { profile ->
            val isSelf = profile.userId == userId
            val initials = buildString {
                append(profile.firstName.firstOrNull()?.uppercaseChar() ?: '?')
                append(profile.lastName.firstOrNull()?.uppercaseChar() ?: '?')
            }
            val displayName = if (isSelf) "You" else "${profile.firstName} ${profile.lastName.first()}."
            GroupMember(id = profile.userId, name = displayName, initials = initials)
        }.ifEmpty {
            listOf(GroupMember(userId, cachedFirstName, initialsOf(cachedFirstName)))
        }

        GroupStudySession(
            id = session.id,
            title = session.title,
            subtitle = session.subtitle ?: "Study session",
            proximityLabel = "",
            members = members
        )
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

    private suspend fun activeCheckInCounts(): Map<String, Int> =
        client.from("spot_occupancy").select().decodeList<OccupancyRow>()
            .associate { it.spotSlug to it.activeCount }

    private suspend fun trendingCheckInCounts(): Map<String, Int> =
        client.from("spot_trending").select().decodeList<TrendingRow>()
            .associate { it.spotSlug to it.checkins7d }

    private suspend fun reviewRowsForSpot(spotSlug: String): List<ReviewRow> =
        client.from("reviews")
            .select {
                filter { eq("spot_slug", spotSlug) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ReviewRow>()

    private suspend fun dbSpotSummary(spotId: String): StudySpotSummary? {
        val activeCount = activeCheckInCounts()[spotId] ?: 0
        return client.from("spots")
            .select { filter { eq("slug", spotId) } }
            .decodeSingleOrNull<SpotDto>()
            ?.toSummary(activeCount = activeCount, childCount = 0)
    }

    private fun SpotDto.toSummary(activeCount: Int, childCount: Int = 0) = StudySpotSummary(
        id = slug,
        name = name,
        badge = occupancyBadge(activeCount),
        building = building,
        parentSlug = parentSlug,
        childCount = childCount,
        latitude = latitude,
        longitude = longitude,
        occupancyPercent = liveOccupancyPercent(activeCount, capacity),
        soloFriendly = soloFriendly,
        groupFriendly = groupFriendly,
        amenities = amenities,
        noiseLevel = noiseLevel,
        lighting = lighting,
        operatingHours = toOperatingHours(),
    )

    private fun SpotDto.toDetail(
        activeCount: Int,
        reviews: List<ReviewRow>
    ): StudySpotDetail {
        val averageRating = reviews
            .takeIf { it.isNotEmpty() }
            ?.map { it.rating }
            ?.average()
            ?.roundToSingleDecimal()
        val reportedOccupancyPercent = reviews.firstNotNullOfOrNull { it.occupancyPercent }
        val liveOccupancyPercent = liveOccupancyPercent(activeCount, capacity)

        return StudySpotDetail(
            id = slug,
            name = name,
            building = building,
            floor = floor,
            badge = occupancyBadge(activeCount),
            parentSlug = parentSlug,
            rating = averageRating,
            studyContextLabel = studyContextLabel(),
            noiseLevel = reviews.firstNotNullOfOrNull { it.noiseLevel },
            lighting = reviews.firstNotNullOfOrNull { it.lighting },
            wifiQuality = reviews.firstNotNullOfOrNull { it.wifiQuality },
            capacity = capacity,
            occupancyPercent = liveOccupancyPercent ?: reportedOccupancyPercent,
            occupancyPercentIsLive = liveOccupancyPercent != null,
            reportedOccupancyPercent = reportedOccupancyPercent,
            peopleHere = activeCount,
            amenities = amenities,
            latitude = latitude,
            longitude = longitude,
            bookingUrl = bookingUrl,
            operatingHours = toOperatingHours(),
        )
    }

    private fun SpotDto.toOperatingHours(): WeeklyOperatingHours? {
        val source = hoursOfOperation ?: return null
        val parsedDays = DayOfWeek.entries.mapNotNull { day ->
            val key = day.name.lowercase()
            val value = source[key] ?: return@mapNotNull null
            val dayObject = value as? JsonObject ?: return@mapNotNull null
            val open24Hours = (dayObject["open_24_hours"] as? JsonPrimitive)
                ?.booleanOrNull == true
            if (open24Hours) {
                return@mapNotNull day to DailyOperatingHours(open24Hours = true)
            }

            val opens = (dayObject["opens"] as? JsonPrimitive)
                ?.contentOrNull
                ?.let(::parseLocalTime)
                ?: return@mapNotNull null
            val closes = (dayObject["closes"] as? JsonPrimitive)
                ?.contentOrNull
                ?.let(::parseLocalTime)
                ?: return@mapNotNull null
            day to DailyOperatingHours(opens = opens, closes = closes)
        }.toMap()

        return parsedDays
            .takeIf { it.isNotEmpty() }
            ?.let { WeeklyOperatingHours(days = it, timeZone = timeZone) }
    }

    private fun SpotDto.studyContextLabel(): String? = when {
        soloFriendly && groupFriendly -> "Solo or group"
        soloFriendly -> "Solo-friendly"
        groupFriendly -> "Group-friendly"
        else -> null
    }
}

private fun List<SpotDto>.childCountsByParentSlug(): Map<String, Int> =
    mapNotNull { it.parentSlug }
        .groupingBy { it }
        .eachCount()

private fun Double.roundToSingleDecimal(): Double =
    kotlin.math.round(this * 10.0) / 10.0

private fun parseLocalTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value) }.getOrNull()

private fun liveOccupancyPercent(activeCount: Int, capacity: Int?): Int? =
    capacity
        ?.takeIf { it > 0 }
        ?.let { kotlin.math.round((activeCount.toDouble() / it) * 100).toInt().coerceIn(0, 100) }

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
    val anonymous: Boolean,
    @SerialName("quality_score") val qualityScore: Int = 0,
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
    val anonymous: Boolean = false,
    @SerialName("quality_score") val qualityScore: Int = 0,
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
                anonymous = draft.anonymous,
                qualityScore = draft.qualityScore,
            )
        )
    }

    override suspend fun getReviewCount(userId: String): Int =
        runCatching {
            client.from("reviews")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ReviewRow>()
                .size
        }.getOrDefault(0)

    override suspend fun getQualityReviewCount(userId: String): Int =
        runCatching {
            client.from("reviews")
                .select { filter { eq("user_id", userId); gte("quality_score", 5) } }
                .decodeList<ReviewRow>()
                .size
        }.getOrDefault(0)
}

@Serializable
private data class StreakDto(
    @SerialName("id") val userId: String,
    @SerialName("login_streak") val loginStreak: Int = 0,
    @SerialName("last_login_date") val lastLoginDate: String? = null,
    @SerialName("longest_login_streak") val longestLoginStreak: Int = 0,
    @SerialName("checkout_count") val checkoutCount: Int = 0,
)

@Serializable
private data class StudySessionInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("spot_id") val spotId: String,
    @SerialName("spot_name") val spotName: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
)

@Serializable
private data class StudySessionRow(
    @SerialName("spot_name") val spotName: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("finished_at") val finishedAt: String,
)

class SupabaseStreakRepository(
    private val client: SupabaseClient
) : StreakRepository {
    override suspend fun recordLogin(userId: String): Int {
        val dto = runCatching {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<StreakDto>()
        }.getOrNull() ?: return 0

        val today = LocalDate.now(ZoneOffset.UTC)
        val lastLogin = dto.lastLoginDate?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

        if (lastLogin == today) return dto.loginStreak

        val newStreak = when {
            lastLogin == null -> 1
            lastLogin == today.minusDays(1) -> dto.loginStreak + 1
            else -> 1
        }
        val newLongest = maxOf(newStreak, dto.longestLoginStreak)

        runCatching {
            client.from("profiles").update({
                set("login_streak", newStreak)
                set("last_login_date", today.toString())
                set("longest_login_streak", newLongest)
            }) { filter { eq("id", userId) } }
        }
        return newStreak
    }

    override suspend fun recordCheckout(
        userId: String,
        spotId: String,
        spotName: String,
        durationSeconds: Int,
    ): Int {
        runCatching {
            client.from("study_sessions").insert(
                StudySessionInsert(userId, spotId, spotName, durationSeconds)
            )
        }
        val current = runCatching {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<StreakDto>()
                ?.checkoutCount ?: 0
        }.getOrDefault(0)
        val newCount = current + 1
        runCatching {
            client.from("profiles").update({
                set("checkout_count", newCount)
            }) { filter { eq("id", userId) } }
        }
        return newCount
    }

    override suspend fun fetchRecentSessions(userId: String): List<CompletedSession> =
        runCatching {
            client.from("study_sessions")
                .select {
                    filter { eq("user_id", userId) }
                    order("finished_at", Order.DESCENDING)
                    limit(20)
                }
                .decodeList<StudySessionRow>()
                .map { row ->
                    CompletedSession(
                        spotName = row.spotName,
                        durationSeconds = row.durationSeconds,
                        finishedAtMillis = runCatching {
                            java.time.Instant.parse(row.finishedAt).toEpochMilli()
                        }.getOrDefault(0L)
                    )
                }
        }.getOrDefault(emptyList())
}

@Serializable
private data class UserBadgeInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("badge_id") val badgeId: String,
)

@Serializable
private data class UserBadgeRow(
    @SerialName("user_id") val userId: String,
    @SerialName("badge_id") val badgeId: String,
    @SerialName("earned_at") val earnedAt: String,
)

class SupabaseBadgeRepository(
    private val client: SupabaseClient
) : BadgeRepository {
    override suspend fun getBadges(userId: String): List<UserBadge> =
        runCatching {
            client.from("user_badges")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserBadgeRow>()
                .mapNotNull { row ->
                    val badgeId = runCatching { BadgeId.valueOf(row.badgeId) }.getOrNull()
                        ?: return@mapNotNull null
                    UserBadge(
                        id = badgeId,
                        earnedAtMillis = runCatching {
                            Instant.parse(row.earnedAt).toEpochMilli()
                        }.getOrDefault(0L)
                    )
                }
        }.getOrDefault(emptyList())

    override suspend fun awardBadge(userId: String, badgeId: BadgeId) {
        runCatching {
            client.from("user_badges").insert(UserBadgeInsert(userId, badgeId.name))
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
