package com.appetizers.spotra.domain.model

enum class StudyMode {
    Solo,
    Group
}

enum class SpotFeatureType {
    Seating,
    Whiteboard,
    Wifi,
    Accessible,
    Outlets,
    Noise,
    Projector,
    NearbyCafe
}

data class SpotFeature(
    val label: String,
    val type: SpotFeatureType
)

data class StudySpotSummary(
    val id: String,
    val name: String,
    val badge: String,
    val parentSlug: String? = null,
    val childCount: Int = 0,
    val distanceMeters: Int? = null,
    val rating: Double? = null,
    val studyContextLabel: String? = null,
    val features: List<SpotFeature> = emptyList(),
    val bestFit: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val occupancyPercent: Int? = null,
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
    val features: List<SpotFeature> = emptyList(),
    val bestFit: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bookingUrl: String? = null,
) {
    fun toSummary() = StudySpotSummary(
        id = id,
        name = name,
        badge = badge,
        parentSlug = parentSlug,
        childCount = childCount,
        distanceMeters = distanceMeters,
        rating = rating,
        studyContextLabel = studyContextLabel,
        features = features,
        bestFit = bestFit,
        latitude = latitude,
        longitude = longitude,
        occupancyPercent = occupancyPercent,
    )
}

data class GroupMember(
    val id: String,
    val name: String,
    val initials: String
)

data class GroupStudySession(
    val id: String,
    val title: String,
    val subtitle: String,
    val proximityLabel: String,
    val members: List<GroupMember>
)

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
    val groupSession: GroupStudySession,
    val groupSpots: List<StudySpotSummary>,
    val mapSpots: List<StudySpotSummary> = emptyList(),
    // Real check-in counts per spot slug over the last 7 days (Explore ranking).
    val trendingCounts: Map<String, Int> = emptyMap()
)

data class CompletedSession(
    val spotName: String,
    val durationSeconds: Int,
    val finishedAtMillis: Long
)
