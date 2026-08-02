package com.appetizers.spotra.domain.model

import kotlin.math.roundToInt

enum class StudyMode {
    Solo,
    Group
}

data class SpotPhoto(
    val url: String,
    val caption: String? = null
)

data class StudySpotSummary(
    val id: String,
    val name: String,
    val badge: String,
    val building: String? = null,
    val parentSlug: String? = null,
    val childCount: Int = 0,
    val distanceMeters: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val photoUrl: String? = null,
    val studyContextLabel: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val occupancyPercent: Int? = null,
    val soloFriendly: Boolean = true,
    val groupFriendly: Boolean = true,
    val amenities: List<String> = emptyList(),
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
    val friendsHere: Int = 0,
    val operatingHours: WeeklyOperatingHours? = null,
)

data class StudySpotDetail(
    val id: String,
    val name: String,
    val building: String,
    val floor: String? = null,
    val badge: String,
    val parentSlug: String? = null,
    val childCount: Int = 0,
    val distanceMeters: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val photos: List<SpotPhoto> = emptyList(),
    val studyContextLabel: String? = null,
    val noiseLevel: String? = null,
    val lighting: String? = null,
    val wifiQuality: String? = null,
    val capacity: Int? = null,
    val occupancyPercent: Int? = null,
    val occupancyPercentIsLive: Boolean = false,
    val reportedOccupancyPercent: Int? = null,
    val peopleHere: Int = 0,
    val amenities: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bookingUrl: String? = null,
    val operatingHours: WeeklyOperatingHours? = null,
) {
    fun toSummary() = StudySpotSummary(
        id = id,
        name = name,
        badge = badge,
        building = building,
        parentSlug = parentSlug,
        childCount = childCount,
        distanceMeters = distanceMeters,
        rating = rating,
        reviewCount = reviewCount,
        photoUrl = photos.firstOrNull()?.url,
        studyContextLabel = studyContextLabel,
        latitude = latitude,
        longitude = longitude,
        occupancyPercent = occupancyPercent,
        noiseLevel = noiseLevel,
        lighting = lighting,
        wifiQuality = wifiQuality,
        amenities = amenities,
        operatingHours = operatingHours,
    )
}

data class SpotOccupancy(
    val spotId: String,
    val activeCount: Int,
    val capacity: Int?,
) {
    val percent: Int?
        get() = capacity
            ?.takeIf { it > 0 }
            ?.let { ((activeCount.toDouble() / it) * 100).roundToInt().coerceIn(0, 100) }

    val badge: String
        get() = when {
            activeCount <= 0 -> "Quiet"
            activeCount < 8 -> "Moderate"
            else -> "Busy"
        }
}

fun StudySpotSummary.withOccupancy(occupancy: SpotOccupancy): StudySpotSummary =
    if (id == occupancy.spotId) {
        copy(
            badge = occupancy.badge,
            occupancyPercent = occupancy.percent,
        )
    } else {
        this
    }

fun StudySpotDetail.withOccupancy(occupancy: SpotOccupancy): StudySpotDetail =
    if (id == occupancy.spotId) {
        copy(
            badge = occupancy.badge,
            capacity = occupancy.capacity,
            occupancyPercent = occupancy.percent ?: reportedOccupancyPercent,
            occupancyPercentIsLive = occupancy.percent != null,
            peopleHere = occupancy.activeCount,
        )
    } else {
        this
    }

data class GroupMember(
    val id: String,
    val name: String,
    val initials: String
)

enum class GroupVisibility {
    Private,
    Public,
}

data class GroupStudySession(
    val id: String,
    val title: String,
    val subtitle: String,
    val proximityLabel: String,
    val members: List<GroupMember>,
    val isOwner: Boolean = false,
    val visibility: GroupVisibility = GroupVisibility.Private,
) {
    /** Ending closes the session for everyone, so only the creator may see that action. */
    val canEndGroup: Boolean
        get() = isOwner
}

data class CheckedInStudent(
    val id: String,
    val initials: String,
    val name: String,
    val detail: String,
    val isSelf: Boolean = false,
    val isFriend: Boolean = false,
    val hasSentMeRequest: Boolean = false
)

data class CheckInSession(
    val id: String,
    val spot: StudySpotSummary,
    val mode: StudyMode,
    val attendees: List<CheckedInStudent>
)

data class HomeSnapshot(
    val userFirstName: String,
    val soloSpot: StudySpotSummary,
    val groupSession: GroupStudySession?,
    val groupSpots: List<StudySpotSummary>,
    val publicGroups: List<GroupStudySession> = emptyList(),
    val mapSpots: List<StudySpotSummary> = emptyList(),
    val trendingCounts: Map<String, Int> = emptyMap(),
)

sealed interface GroupSessionEvent {
    data class MembersChanged(val members: List<GroupMember>) : GroupSessionEvent
    data object Ended : GroupSessionEvent
}

data class GroupInvite(
    val id: String,
    val groupSessionId: String,
    val groupTitle: String,
    val inviterName: String,
)

sealed interface EmailInviteResult {
    data class Added(val member: GroupMember) : EmailInviteResult
    data class InviteSent(val recipientName: String) : EmailInviteResult
}

data class CompletedSession(
    val spotName: String,
    val durationSeconds: Int,
    val finishedAtMillis: Long
)
