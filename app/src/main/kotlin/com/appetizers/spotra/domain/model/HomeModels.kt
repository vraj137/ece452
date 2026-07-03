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
    val distanceMeters: Int? = null,
    val rating: Double? = null,
    val studyContextLabel: String? = null,
    val features: List<SpotFeature> = emptyList(),
    val bestFit: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

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
    val mapSpots: List<StudySpotSummary> = emptyList()
)

data class CompletedSession(
    val spotName: String,
    val durationSeconds: Int,
    val finishedAtMillis: Long
)
