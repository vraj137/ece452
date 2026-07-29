package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.DailyOperatingHours
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotOccupancy
import com.appetizers.spotra.domain.model.SpotPhoto
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
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.SpotReviewAggregator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlin.math.roundToInt
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private fun Double.roundToSingleDecimal(): Double = (this * 10).roundToInt() / 10.0

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
    @SerialName("wifi_quality") val wifiQuality: String? = null,
    @SerialName("hours_of_operation") val hoursOfOperation: JsonObject? = null,
    @SerialName("time_zone") val timeZone: String = WATERLOO_TIME_ZONE,
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
    val visibility: String = "private",
    @SerialName("created_by") val createdBy: String,
    @SerialName("ended_at") val endedAt: String? = null,
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
private data class OccupancyBroadcast(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("active_count") val activeCount: Int,
    val capacity: Int? = null,
)

@Serializable
private data class TrendingRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("checkins_7d") val checkins7d: Int
)

@Serializable
private data class SpotPhotoRow(
    @SerialName("spot_slug") val spotSlug: String,
    val url: String,
    val caption: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
private data class SpotReviewStatsRow(
    @SerialName("spot_slug") val spotSlug: String,
    @SerialName("average_rating") val averageRating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("noise_level") val noiseLevel: String? = null,
    val lighting: String? = null,
    @SerialName("wifi_quality") val wifiQuality: String? = null,
)

@Serializable
private data class AttendeeProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val program: String = "",
    @SerialName("is_friend") val isFriend: Boolean = false,
    @SerialName("has_sent_me_request") val hasSentMeRequest: Boolean = false,
)

@Serializable
private data class SafeProfileRow(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val program: String = "",
    @SerialName("study_term") val studyTerm: String? = null,
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

    override fun observeOccupancy(): Flow<SpotOccupancy> = channelFlow {
        val realtimeChannel = client.channel(OCCUPANCY_CHANNEL) {
            isPrivate = true
        }
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            realtimeChannel
                .broadcastFlow<OccupancyBroadcast>(OCCUPANCY_EVENT)
                .collect { update ->
                    send(
                        SpotOccupancy(
                            spotId = update.spotSlug,
                            activeCount = update.activeCount,
                            capacity = update.capacity,
                        )
                    )
                }
        }

        try {
            withTimeout(OCCUPANCY_SUBSCRIBE_TIMEOUT_MILLIS) {
                realtimeChannel.subscribe(blockUntilSubscribed = true)
            }
            awaitCancellation()
        } finally {
            collector.cancelAndJoin()
            client.realtime.removeChannel(realtimeChannel)
        }
    }.buffer(Channel.CONFLATED)

    override suspend fun loadHome(): HomeSnapshot = coroutineScope {
        val firstNameDeferred = async { currentFirstName() }
        val activeCountsDeferred = async { activeCheckInCounts() }
        val trendingDeferred = async {
            runCatching { trendingCheckInCounts() }.getOrDefault(emptyMap())
        }
        val reviewStatsDeferred = async {
            runCatching { spotReviewStats() }.getOrDefault(emptyMap())
        }
        val photosDeferred = async {
            runCatching { spotPhotosBySlug() }.getOrDefault(emptyMap())
        }
        val dbSpotsDeferred = async {
            client.from("spots").select().decodeList<SpotDto>()
        }
        val groupDataDeferred = async {
            runCatching { loadGroupData() }.getOrDefault(GroupData())
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
        val reviewStats = reviewStatsDeferred.await()
        val photosBySlug = photosDeferred.await()
        val dbSpots = dbSpotsDeferred.await()
        val childCounts = dbSpots.childCountsByParentSlug()
        val parentSpots = dbSpots.filter { it.parentSlug == null }

        val baseSummaries = parentSpots.map { spot ->
            spot.toSummary(
                activeCount = activeCounts[spot.slug] ?: 0,
                childCount = childCounts[spot.slug] ?: 0,
                stats = reviewStats[spot.slug],
                coverPhotoUrl = photosBySlug[spot.slug]?.firstOrNull()?.url
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

        val groupData = groupDataDeferred.await()

        HomeSnapshot(
            userFirstName = cachedFirstName,
            soloSpot = soloSpot,
            groupSession = groupData.activeGroup,
            groupSpots = groupSpots,
            publicGroups = groupData.publicGroups,
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
        val photosDeferred = async {
            runCatching { spotPhotosBySlug()[spotId].orEmpty() }.getOrDefault(emptyList())
        }

        val activeCount = activeCountsDeferred.await()[spotId] ?: 0
        val reviews = reviewsDeferred.await()
        val photos = photosDeferred.await()
        val dbSpot = spotDeferred.await()

        requireNotNull(dbSpot) { "Study spot not found in Supabase: $spotId" }
            .toDetail(activeCount, reviews, photos)
    }

    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = coroutineScope {
        val childrenDeferred = async {
            client.from("spots")
                .select { filter { eq("parent_slug", parentSpotId) } }
                .decodeList<SpotDto>()
        }
        val activeCountsDeferred = async { activeCheckInCounts() }
        val photosDeferred = async {
            runCatching { spotPhotosBySlug() }.getOrDefault(emptyMap())
        }

        val children = childrenDeferred.await()
        val activeCounts = activeCountsDeferred.await()
        val photosBySlug = photosDeferred.await()

        children
            .sortedWith(compareBy<SpotDto> { it.floor.orEmpty() }.thenBy { it.name })
            .map { spot ->
                spot.toDetail(
                    activeCount = activeCounts[spot.slug] ?: 0,
                    reviews = emptyList(),
                    photos = photosBySlug[spot.slug].orEmpty()
                )
            }
    }

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?,
        latitude: Double,
        longitude: Double,
    ): CheckInSession {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to check in.")
        val inserted = client.postgrest.rpc(
            "start_verified_check_in",
            buildJsonObject {
                put("p_spot_slug", spotId)
                put("p_mode", if (mode == StudyMode.Group) "group" else "solo")
                put("p_group_session_id", groupSessionId?.let(::JsonPrimitive) ?: JsonNull)
                put("p_latitude", latitude)
                put("p_longitude", longitude)
            }
        ).decodeSingle<CheckInRow>()

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

    private suspend fun loadCoAttendees(selfId: String, spotId: String): List<CheckedInStudent> =
        client.postgrest.rpc(
            "session_attendees",
            buildJsonObject { put("p_spot_slug", spotId) }
        ).decodeList<AttendeeProfileRow>().filter { it.id != selfId }.map { p ->
            CheckedInStudent(
                id = p.id,
                initials = buildString {
                    p.firstName.firstOrNull()?.uppercaseChar()?.let { append(it) }
                    p.lastName.firstOrNull()?.uppercaseChar()?.let { append(it) }
                }.ifBlank { "?" },
                name = "${p.firstName} ${p.lastName.firstOrNull() ?: ""}.",
                detail = p.program.ifBlank { "Studying here" },
                isFriend = p.isFriend,
                hasSentMeRequest = p.hasSentMeRequest,
            )
        }

    override suspend fun checkOut(sessionId: String) {
        client.from("check_ins").update({
            set("ended_at", Instant.now().toString())
        }) {
            filter { eq("id", sessionId) }
        }
    }

    override suspend fun createGroup(
        title: String,
        visibility: GroupVisibility
    ): GroupStudySession {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to create a group.")
        val cleanTitle = title.trim()
        require(cleanTitle.length in 2..50) { "Group names must be between 2 and 50 characters." }
        require(loadActiveGroupSession() == null) {
            "Leave your current group before creating another one."
        }

        val created = client.postgrest.rpc(
            "create_group_session",
            buildJsonObject {
                put("p_title", cleanTitle)
                put("p_visibility", visibility.toDatabaseValue())
            }
        ).decodeSingle<GroupSessionDto>()
        activeGroupSessionId = created.id
        return loadGroupSession(created, userId)
    }

    override suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to join a public group.")
        require(loadActiveGroupSession() == null) {
            "Leave your current group before joining another one."
        }

        val session = client.from("group_sessions")
            .select {
                filter {
                    eq("id", groupSessionId)
                    eq("visibility", "public")
                    exact("ended_at", null)
                }
            }
            .decodeSingleOrNull<GroupSessionDto>()
            ?: error("This public group is no longer open.")

        client.from("group_session_members").insert(
            GroupSessionMemberInsert(
                sessionId = session.id,
                userId = userId,
                role = "member"
            )
        )
        activeGroupSessionId = session.id
        return loadGroupSession(session, userId)
    }

    override suspend fun leaveGroup(groupSessionId: String) {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("You need to be signed in to leave a group.")
        require(groupSessionId.isNotBlank()) { "A live group session is required." }

        val session = client.from("group_sessions")
            .select { filter { eq("id", groupSessionId) } }
            .decodeSingleOrNull<GroupSessionDto>()
            ?: error("This group is no longer active.")

        if (session.createdBy == userId) {
            client.from("group_sessions").update({
                set("ended_at", Instant.now().toString())
            }) {
                filter {
                    eq("id", groupSessionId)
                    exact("ended_at", null)
                }
            }
        } else {
            client.from("group_session_members").delete {
                filter {
                    eq("session_id", groupSessionId)
                    eq("user_id", userId)
                }
            }
        }

        if (activeGroupSessionId == groupSessionId) {
            activeGroupSessionId = null
        }
    }

    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember {
        require(groupSessionId.isNotBlank()) { "A live group session is required before inviting members." }
        require(activeGroupSessionId == null || activeGroupSessionId == groupSessionId) {
            "This group session is no longer active. Refresh and try again."
        }

        if (!inviteText.contains("@")) {
            error("Enter the email address associated with their Spotra account to invite them.")
        }
        val profile = client.postgrest.rpc(
            "invite_group_member_by_email",
            buildJsonObject {
                put("p_group_id", groupSessionId)
                put("p_email", inviteText.trim())
            }
        ).decodeSingle<SafeProfileRow>()
        return run {
            val initials = buildString {
                append(profile.firstName.firstOrNull()?.uppercaseChar() ?: '?')
                append(profile.lastName.firstOrNull()?.uppercaseChar() ?: '?')
            }
            val displayName = "${profile.firstName} ${profile.lastName.firstOrNull() ?: ""}."
            GroupMember(id = profile.id, name = displayName, initials = initials)
        }
    }

    private data class GroupData(
        val activeGroup: GroupStudySession? = null,
        val publicGroups: List<GroupStudySession> = emptyList(),
    )

    private suspend fun loadGroupData(): GroupData {
        val activeGroup = loadActiveGroupSession()
        return GroupData(
            activeGroup = activeGroup,
            publicGroups = loadPublicGroups(excludingId = activeGroup?.id),
        )
    }

    private suspend fun loadActiveGroupSession(): GroupStudySession? {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return null

        val sessionIds = client.from("group_session_members")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<GroupSessionMemberRow>()
            .map { it.sessionId }
        if (sessionIds.isEmpty()) {
            activeGroupSessionId = null
            return null
        }

        val session = client.from("group_sessions")
            .select {
                filter {
                    isIn("id", sessionIds)
                    exact("ended_at", null)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<GroupSessionDto>()
            .firstOrNull()
            ?: run {
                activeGroupSessionId = null
                return null
            }

        activeGroupSessionId = session.id
        return loadGroupSession(session, userId)
    }

    private suspend fun loadPublicGroups(excludingId: String?): List<GroupStudySession> =
        client.from("group_sessions")
            .select {
                filter {
                    eq("visibility", "public")
                    exact("ended_at", null)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<GroupSessionDto>()
            .filterNot { it.id == excludingId }
            .map { session ->
                GroupStudySession(
                    id = session.id,
                    title = session.title,
                    subtitle = session.subtitle ?: "Open study group",
                    proximityLabel = "",
                    members = emptyList(),
                    isOwner = false,
                    visibility = GroupVisibility.Public,
                )
            }

    private suspend fun loadGroupSession(
        session: GroupSessionDto,
        userId: String
    ): GroupStudySession = coroutineScope {
        val memberUserIds = client.from("group_session_members")
            .select { filter { eq("session_id", session.id) } }
            .decodeList<GroupSessionMemberRow>()
            .map { it.userId }

        val profiles = if (memberUserIds.isEmpty()) {
            emptyList()
        } else {
            client.postgrest.rpc(
                "safe_profiles",
                buildJsonObject {
                    put("p_ids", kotlinx.serialization.json.JsonArray(
                        memberUserIds.map(::JsonPrimitive)
                    ))
                    put("p_limit", 50)
                }
            ).decodeList<SafeProfileRow>()
        }
        val ownProfile = if (userId in memberUserIds) {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<UserProfileDto>()
                ?.let {
                    SafeProfileRow(
                        id = it.userId,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        program = it.program,
                        studyTerm = it.studyTerm?.label,
                    )
                }
        } else {
            null
        }
        val allProfiles = (profiles + listOfNotNull(ownProfile)).distinctBy { it.id }

        val members = allProfiles.map { profile ->
            val isSelf = profile.id == userId
            val initials = buildString {
                append(profile.firstName.firstOrNull()?.uppercaseChar() ?: '?')
                append(profile.lastName.firstOrNull()?.uppercaseChar() ?: '?')
            }
            val displayName = if (isSelf) "You" else "${profile.firstName} ${profile.lastName.first()}."
            GroupMember(id = profile.id, name = displayName, initials = initials)
        }.ifEmpty {
            listOf(GroupMember(userId, cachedFirstName, initialsOf(cachedFirstName)))
        }

        GroupStudySession(
            id = session.id,
            title = session.title,
            subtitle = session.subtitle ?: "Study session",
            proximityLabel = "",
            members = members,
            isOwner = session.createdBy == userId,
            visibility = session.visibility.toGroupVisibility(),
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

    private suspend fun spotReviewStats(): Map<String, SpotReviewStatsRow> =
        client.from("spot_review_stats").select().decodeList<SpotReviewStatsRow>()
            .associateBy { it.spotSlug }

    /** All curated photos, grouped by spot and already in cover-first order. */
    private suspend fun spotPhotosBySlug(): Map<String, List<SpotPhoto>> =
        client.from("spot_photos")
            .select { order("sort_order", Order.ASCENDING) }
            .decodeList<SpotPhotoRow>()
            .groupBy { it.spotSlug }
            .mapValues { (_, rows) -> rows.map { SpotPhoto(url = it.url, caption = it.caption) } }

    private suspend fun reviewRowsForSpot(spotSlug: String): List<ReviewRow> =
        client.postgrest.rpc(
            "list_spot_reviews",
            buildJsonObject { put("p_spot_slug", spotSlug) }
        ).decodeList<ReviewRow>()

    private suspend fun dbSpotSummary(spotId: String): StudySpotSummary? {
        val activeCount = activeCheckInCounts()[spotId] ?: 0
        val stats = runCatching { spotReviewStats()[spotId] }.getOrNull()
        return client.from("spots")
            .select { filter { eq("slug", spotId) } }
            .decodeSingleOrNull<SpotDto>()
            ?.toSummary(activeCount = activeCount, childCount = 0, stats = stats)
    }

    private fun GroupVisibility.toDatabaseValue(): String = when (this) {
        GroupVisibility.Private -> "private"
        GroupVisibility.Public -> "public"
    }

    private fun String.toGroupVisibility(): GroupVisibility =
        if (equals("public", ignoreCase = true)) GroupVisibility.Public else GroupVisibility.Private

    /**
     * [stats] carries the crowdsourced consensus from `spot_review_stats`. Where students have
     * reported a value it wins; the seeded column on `spots` stays as the fallback so a spot with
     * no reviews yet still ranks sensibly on the Explore leaderboards.
     */
    private fun SpotDto.toSummary(
        activeCount: Int,
        childCount: Int = 0,
        stats: SpotReviewStatsRow? = null,
        coverPhotoUrl: String? = null,
    ) = StudySpotSummary(
        id = slug,
        name = name,
        badge = SpotOccupancy(slug, activeCount, capacity).badge,
        building = building,
        parentSlug = parentSlug,
        childCount = childCount,
        rating = stats?.averageRating,
        reviewCount = stats?.reviewCount ?: 0,
        photoUrl = coverPhotoUrl,
        latitude = latitude,
        longitude = longitude,
        occupancyPercent = SpotOccupancy(slug, activeCount, capacity).percent,
        soloFriendly = soloFriendly,
        groupFriendly = groupFriendly,
        amenities = amenities,
        noiseLevel = stats?.noiseLevel ?: noiseLevel,
        lighting = stats?.lighting ?: lighting,
        wifiQuality = stats?.wifiQuality ?: wifiQuality,
        operatingHours = toOperatingHours(),
    )

    private fun SpotDto.toDetail(
        activeCount: Int,
        reviews: List<ReviewRow>,
        photos: List<SpotPhoto> = emptyList()
    ): StudySpotDetail {
        val averageRating = reviews
            .takeIf { it.isNotEmpty() }
            ?.map { it.rating }
            ?.average()
            ?.roundToSingleDecimal()
        val occupancyValues = reviews.mapNotNull { it.occupancyPercent }
        val reportedOccupancyPercent = if (occupancyValues.isEmpty()) null
            else occupancyValues.average().roundToInt().coerceIn(0, 100)
        // Same aggregation the spot_review_stats view applies for the leaderboards, so a spot's
        // detail screen and its Explore ranking never disagree.
        val aggregates = SpotReviewAggregator.aggregate(
            ratings = reviews.map { it.rating },
            noiseLevels = reviews.map { it.noiseLevel },
            lightings = reviews.map { it.lighting },
            wifiQualities = reviews.map { it.wifiQuality },
        )
        val occupancy = SpotOccupancy(slug, activeCount, capacity)
        val liveOccupancyPercent = occupancy.percent

        return StudySpotDetail(
            id = slug,
            name = name,
            building = building,
            floor = floor,
            badge = occupancy.badge,
            parentSlug = parentSlug,
            rating = aggregates.averageRating,
            reviewCount = aggregates.reviewCount,
            photos = photos,
            studyContextLabel = studyContextLabel(),
            noiseLevel = aggregates.noiseLevel ?: noiseLevel,
            lighting = aggregates.lighting ?: lighting,
            wifiQuality = aggregates.wifiQuality ?: wifiQuality,
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

private fun parseLocalTime(value: String): LocalTime? =
    runCatching { LocalTime.parse(value) }.getOrNull()

private fun initialsOf(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
        .take(2)

private const val OCCUPANCY_CHANNEL = "spot-occupancy"
private const val OCCUPANCY_EVENT = "occupancy_changed"
private const val OCCUPANCY_SUBSCRIBE_TIMEOUT_MILLIS = 15_000L

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
    @SerialName("is_owner") val isOwner: Boolean = false,
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
            anonymous = anonymous,
            isOwnedByCurrentUser = isOwner,
        )
    }
}

class SupabaseReviewRepository(
    private val client: SupabaseClient
) : ReviewRepository {
    override suspend fun reviewsFor(spotSlug: String): List<Review> =
        client.postgrest.rpc(
            "list_spot_reviews",
            buildJsonObject { put("p_spot_slug", spotSlug) }
        )
            .decodeList<ReviewRow>()
            .map { it.toReview() }

    override suspend fun submit(draft: ReviewDraft) {
        if (client.auth.currentUserOrNull() == null) {
            error("You need to be signed in to review.")
        }
        client.postgrest.rpc(
            "submit_review",
            buildJsonObject {
                put("p_spot_slug", draft.spotSlug)
                put("p_rating", draft.rating)
                put("p_noise_level", draft.noiseLevel?.let(::JsonPrimitive) ?: JsonNull)
                put("p_lighting", draft.lighting?.let(::JsonPrimitive) ?: JsonNull)
                put("p_wifi_quality", draft.wifiQuality?.let(::JsonPrimitive) ?: JsonNull)
                put("p_occupancy_percent", draft.occupancyPercent?.let(::JsonPrimitive) ?: JsonNull)
                put("p_comment", draft.comment?.let(::JsonPrimitive) ?: JsonNull)
                put("p_anonymous", draft.anonymous)
                put("p_quality_score", draft.qualityScore)
            }
        )
    }

    override suspend fun update(reviewId: String, draft: ReviewDraft) {
        client.postgrest.rpc(
            "update_own_review",
            buildJsonObject {
                put("p_review_id", reviewId)
                put("p_rating", draft.rating)
                put("p_noise_level", draft.noiseLevel?.let(::JsonPrimitive) ?: JsonNull)
                put("p_lighting", draft.lighting?.let(::JsonPrimitive) ?: JsonNull)
                put("p_wifi_quality", draft.wifiQuality?.let(::JsonPrimitive) ?: JsonNull)
                put("p_occupancy_percent", draft.occupancyPercent?.let(::JsonPrimitive) ?: JsonNull)
                put("p_comment", draft.comment?.let(::JsonPrimitive) ?: JsonNull)
                put("p_anonymous", draft.anonymous)
                put("p_quality_score", draft.qualityScore)
            }
        )
    }

    override suspend fun delete(reviewId: String) {
        client.postgrest.rpc(
            "delete_own_review",
            buildJsonObject { put("p_review_id", reviewId) }
        )
    }

    override suspend fun getReviewCount(userId: String): Int =
        runCatching {
            client.postgrest.rpc(
                "review_count",
                buildJsonObject { put("p_quality_only", false) }
            ).decodeAs<Int>()
        }.getOrDefault(0)

    override suspend fun getQualityReviewCount(userId: String): Int =
        runCatching {
            client.postgrest.rpc(
                "review_count",
                buildJsonObject { put("p_quality_only", true) }
            ).decodeAs<Int>()
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
