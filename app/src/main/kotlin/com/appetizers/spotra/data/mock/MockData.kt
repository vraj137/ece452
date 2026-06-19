package com.appetizers.spotra.data.mock

import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.SpotFeature
import com.appetizers.spotra.domain.model.SpotFeatureType
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserProfile

// Single source of truth for all mock/debug data used across the app.
// Screens and debug repositories both draw from here so adding a spot
// only needs to happen in one place.

data class MockSpot(
    val id: String,
    val name: String,
    val building: String,
    val shortLabel: String,       // abbreviated label for map pins
    val latitude: Double,
    val longitude: Double,
    val mapPinX: Float,           // fractional x position on placeholder map (0..1)
    val mapPinY: Float,           // fractional y position on placeholder map (0..1)
    val distanceMeters: Int,
    val rating: Double,
    val badge: String,            // Quiet / Silent / Moderate / Lively / Best fit
    val studyContextLabel: String? = null,
    val noiseLevel: String,       // Low / Silent / Moderate / Lively
    val lighting: String,         // Good / Natural / Bright / Poor
    val fullPercent: Int,
    val peopleHere: Int,
    val amenities: List<String>,
    val features: List<SpotFeature> = emptyList(),
    val bestFit: Boolean = false,
    val checkInsThisWeek: Int,
    val reviews: List<MockReview> = emptyList()
) {
    fun toSummary() = StudySpotSummary(
        id = id,
        name = name,
        badge = badge,
        distanceMeters = distanceMeters,
        rating = rating,
        studyContextLabel = studyContextLabel,
        features = features,
        bestFit = bestFit,
        latitude = latitude,
        longitude = longitude
    )
}

data class MockReview(
    val reviewerName: String,
    val reviewerInitials: String,
    val reviewerId: String,
    val rating: Int,
    val comment: String? = null
)

object MockData {

    // ── User ─────────────────────────────────────────────────────────────────

    val user = UserProfile(
        userId = "debug-user-001",
        firstName = "Vraj",
        lastName = "Patel",
        email = "vpatel@uwaterloo.ca",
        program = "Systems Design Engineering",
        studyTerm = StudyTerm.THREE_A,
        onboardingComplete = true
    )

    val selfInitials = "${user.firstName.first()}${user.lastName.first()}"   // "VP"
    val selfDisplayName = "${user.firstName} ${user.lastName.first()}."      // "Vraj P."

    // ── Group session ─────────────────────────────────────────────────────────

    val groupSession = GroupStudySession(
        id = "app-etizers-cs341",
        title = "app-etizers study sesh",
        subtitle = "CS 341 finals prep",
        proximityLabel = "all within 10 min",
        members = listOf(
            GroupMember("you", selfDisplayName, selfInitials),
            GroupMember("akshat", "Akshat J.", "AJ"),
            GroupMember("eric", "Eric Z.", "EZ"),
            GroupMember("raghav", "Raghav V.", "RV"),
            GroupMember("pavan", "Pavan J.", "PJ")
        )
    )

    // ── Study spots ───────────────────────────────────────────────────────────
    // UW campus coordinates (WGS-84).  mapPinX/Y are fractional positions on
    // the placeholder grid in HomeScreen (0..1 in each axis).

    val spots: List<MockSpot> = listOf(

        MockSpot(
            id = "e7-study-hall",
            name = "E7 Study Hall",
            building = "Engineering 7, Room 2101",
            shortLabel = "E7 Hall",
            latitude = 43.4714,
            longitude = -80.5397,
            mapPinX = 0.64f,
            mapPinY = 0.20f,
            distanceMeters = 120,
            rating = 4.8,
            badge = "Quiet",
            studyContextLabel = "Solo-friendly",
            noiseLevel = "Low",
            lighting = "Good",
            fullPercent = 26,
            peopleHere = 6,
            amenities = listOf("Fast Wi-Fi", "Outlets", "Accessible", "Soft seating", "Near café"),
            checkInsThisWeek = 142,
            reviews = listOf(
                MockReview("Raghav V.", "RV", "raghav", 5, "Great for solo work, very quiet!"),
                MockReview("Mei L.", "ML", "mei", 4, "Good lighting, slightly warm though.")
            )
        ),

        MockSpot(
            id = "dc-library-3f",
            name = "DC Library 3F",
            building = "Davis Centre, Floor 3",
            shortLabel = "DC Lib",
            latitude = 43.4727,
            longitude = -80.5427,
            mapPinX = 0.33f,
            mapPinY = 0.35f,
            distanceMeters = 340,
            rating = 4.6,
            badge = "Silent",
            studyContextLabel = "Quiet zone",
            noiseLevel = "Silent",
            lighting = "Natural",
            fullPercent = 45,
            peopleHere = 14,
            amenities = listOf("Fast Wi-Fi", "Outlets", "Accessible", "Near café"),
            features = listOf(
                SpotFeature("Seats 30", SpotFeatureType.Seating),
                SpotFeature("Fast Wi-Fi", SpotFeatureType.Wifi),
                SpotFeature("Near café", SpotFeatureType.NearbyCafe)
            ),
            checkInsThisWeek = 98,
            reviews = listOf(
                MockReview("Mei L.", "ML", "mei", 5, "Best quiet spot on campus!"),
                MockReview("Sam C.", "SC", "sam", 4)
            )
        ),

        MockSpot(
            id = "slc-boardroom-2a",
            name = "SLC Boardroom 2A",
            building = "Student Life Centre, Room 2A",
            shortLabel = "SLC 2A",
            latitude = 43.4721,
            longitude = -80.5461,
            mapPinX = 0.07f,
            mapPinY = 0.65f,
            distanceMeters = 210,
            rating = 4.3,
            badge = "Best fit",
            noiseLevel = "Moderate",
            lighting = "Bright",
            fullPercent = 12,
            peopleHere = 4,
            amenities = listOf("Fast Wi-Fi", "Whiteboard", "Accessible", "Soft seating"),
            features = listOf(
                SpotFeature("Seats 8", SpotFeatureType.Seating),
                SpotFeature("Whiteboard", SpotFeatureType.Whiteboard),
                SpotFeature("Fast Wi-Fi", SpotFeatureType.Wifi),
                SpotFeature("Accessible", SpotFeatureType.Accessible)
            ),
            bestFit = true,
            checkInsThisWeek = 87,
            reviews = listOf(MockReview("Akshat J.", "AJ", "akshat", 4, "Great room for group sessions."))
        ),

        MockSpot(
            id = "mc-atrium",
            name = "MC Atrium",
            building = "Mathematics & Computer, Atrium",
            shortLabel = "MC Atrium",
            latitude = 43.4731,
            longitude = -80.5453,
            mapPinX = 0.30f,
            mapPinY = 0.31f,
            distanceMeters = 620,
            rating = 4.1,
            badge = "Moderate",
            noiseLevel = "Moderate",
            lighting = "Natural",
            fullPercent = 73,
            peopleHere = 22,
            amenities = listOf("Fast Wi-Fi", "Outlets", "Near café"),
            features = listOf(
                SpotFeature("Seats 30+", SpotFeatureType.Seating),
                SpotFeature("Near café", SpotFeatureType.NearbyCafe),
                SpotFeature("Fast Wi-Fi", SpotFeatureType.Wifi)
            ),
            checkInsThisWeek = 65,
            reviews = listOf(MockReview("Sam C.", "SC", "sam", 4, "Busy but good vibes."))
        ),

        MockSpot(
            id = "e5-collab-lab",
            name = "E5 Collab Lab",
            building = "Engineering 5, Collaboration Lab",
            shortLabel = "E5 Lab",
            latitude = 43.4716,
            longitude = -80.5404,
            mapPinX = 0.63f,
            mapPinY = 0.64f,
            distanceMeters = 280,
            rating = 3.8,
            badge = "Lively",
            noiseLevel = "Lively",
            lighting = "Poor",
            fullPercent = 89,
            peopleHere = 18,
            amenities = listOf("Outlets", "Accessible"),
            features = listOf(
                SpotFeature("Seats 6", SpotFeatureType.Seating),
                SpotFeature("Outlets", SpotFeatureType.Outlets),
                SpotFeature("Moderate noise", SpotFeatureType.Noise)
            ),
            checkInsThisWeek = 51,
            reviews = listOf(MockReview("Nora K.", "NK", "nora", 3, "Gets crowded fast, but decent outlets."))
        ),

        MockSpot(
            id = "dp-library",
            name = "DP Library",
            building = "Dana Porter Library, 4th Floor",
            shortLabel = "DP Lib",
            latitude = 43.4707,
            longitude = -80.5443,
            mapPinX = 0.66f,
            mapPinY = 0.64f,
            distanceMeters = 890,
            rating = 4.5,
            badge = "Silent",
            studyContextLabel = "Quiet zone",
            noiseLevel = "Silent",
            lighting = "Bright",
            fullPercent = 33,
            peopleHere = 9,
            amenities = listOf("Fast Wi-Fi", "Outlets", "Accessible", "Soft seating"),
            checkInsThisWeek = 43,
            reviews = listOf(MockReview("Julia W.", "JW", "julia", 5, "Underrated gem on campus!"))
        )
    )

    // ── Convenience lookups ───────────────────────────────────────────────────

    fun spotById(id: String): MockSpot? = spots.find { it.id == id }

    // Spots that appear in the group mode spot list, ordered by fit.
    val groupSpots: List<MockSpot> = listOf(
        spotById("slc-boardroom-2a")!!,
        spotById("e5-collab-lab")!!,
        spotById("mc-atrium")!!
    )

    // Solo-mode map pins: (spotId, selectedByDefault)
    val soloMapPins: List<Pair<String, Boolean>> = listOf(
        "e7-study-hall" to true,
        "dc-library-3f" to false,
        "slc-boardroom-2a" to false,
        "dp-library" to false
    )

    // Group-mode map pins: (spotId, selectedByDefault)
    val groupMapPins: List<Pair<String, Boolean>> = listOf(
        "dc-library-3f" to true,
        "e7-study-hall" to false,
        "slc-boardroom-2a" to false,
        "e5-collab-lab" to false
    )

    // Students checked into the solo spot (used by DebugHomeRepository)
    val soloCheckInStudents = listOf(
        Triple("akshat", "AJ", "Akshat J."),
        Triple("eric", "EZ", "Eric Z."),
        Triple("raghav", "RV", "Raghav V."),
        Triple("mei", "ML", "Mei L."),
        Triple("nora", "NK", "Nora K."),
        Triple("sam", "SC", "Sam C."),
        Triple("tina", "TP", "Tina P."),
        Triple("omar", "OH", "Omar H."),
        Triple("julia", "JW", "Julia W."),
        Triple("dev", "DS", "Dev S.")
    )
}
