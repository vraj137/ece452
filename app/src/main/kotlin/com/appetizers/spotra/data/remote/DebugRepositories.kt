package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.SpotFeature
import com.appetizers.spotra.domain.model.SpotFeatureType
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.SocialRepository
import com.appetizers.spotra.domain.model.SocialSnapshot
import com.appetizers.spotra.domain.model.SocialUser
import com.appetizers.spotra.domain.model.StudyTerm

/**
 * Used in DEBUG builds when Supabase credentials are absent.
 * Accepts any 6-digit OTP and maintains an in-memory session so the full
 * onboarding flow can be exercised without a live backend.
 */
class DebugAuthRepository : AuthRepository {
    private var session: AuthUser? = null

    override suspend fun currentUser(): AuthUser? = session

    override suspend fun sendOtp(email: String, createUser: Boolean) = Unit

    override suspend fun verifyOtp(email: String, token: String): AuthUser {
        val user = AuthUser(id = "debug-${email.hashCode()}", email = email)
        session = user
        return user
    }

    override suspend fun signOut() {
        session = null
    }
}

class DebugProfileRepository : ProfileRepository {
    private var profile: UserProfile? = null

    override suspend fun getProfile(userId: String): UserProfile? = profile

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile = profile
    }
}

class DebugSocialRepository : SocialRepository {
    private val friends = mutableListOf(
        SocialUser("raghav", "Raghav Verma", "Computer Engineering", StudyTerm.TWO_A)
    )
    private val incoming = mutableListOf(
        SocialUser("vishvam", "Vishvam Patel", "Computer Engineering", StudyTerm.TWO_A)
    )
    private val suggestions = mutableListOf(
        SocialUser("edmond", "Edmond Yang", "Computer Engineering", StudyTerm.TWO_A),
        SocialUser("akshat", "Akshat Jawne", "Computer Engineering", StudyTerm.TWO_A)
    )
    private val outgoing = mutableSetOf<String>()

    override suspend fun loadSocial() = SocialSnapshot(friends, incoming, suggestions, outgoing)

    override suspend fun sendFriendRequest(recipientId: String) {
        outgoing += recipientId
    }

    override suspend fun acceptFriendRequest(requesterId: String) {
        incoming.firstOrNull { it.id == requesterId }?.let { friend ->
            incoming.remove(friend)
            friends += friend
        }
    }
}

class DebugHomeRepository : HomeRepository {
    private var groupSession = defaultGroupSession()

    override suspend fun loadHome(): HomeSnapshot =
        HomeSnapshot(
            userFirstName = "Vraj",
            soloSpot = loadHomeSoloSpot(),
            groupSession = groupSession,
            groupSpots = groupSpotCandidates(),
            mapSpots = mapSpots()
        )

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession {
        val spot = spotFor(spotId)
        return CheckInSession(
            id = "debug-${spot.id}-${mode.name}",
            spot = spot,
            mode = mode,
            attendees = checkedInStudentsFor(mode, groupSession.members)
        )
    }

    override suspend fun checkOut(sessionId: String) = Unit

    override suspend fun sendBuddyRequest(studentId: String) = Unit

    override suspend fun inviteToGroup(
        groupSessionId: String,
        inviteText: String
    ): GroupMember {
        val initials = inviteText
            .split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }

        return GroupMember(
            id = "invite-${inviteText.lowercase().replace(" ", "-")}-${groupSession.members.size}",
            name = inviteText,
            initials = initials.take(2)
        ).also { member ->
            groupSession = groupSession.copy(members = groupSession.members + member)
        }
    }

    private fun spotFor(spotId: String): StudySpotSummary =
        (listOf(loadHomeSoloSpot()) + groupSpotCandidates() + mapSpots())
            .associateBy { it.id }
            .values
            .firstOrNull { it.id == spotId }
            ?: error("Unknown study spot: $spotId")

    private fun loadHomeSoloSpot() = StudySpotSummary(
        id = "e7-study-hall",
        name = "E7 Study Hall",
        badge = "Quiet",
        distanceMeters = 120,
        rating = 4.8,
        studyContextLabel = "Solo-friendly",
        latitude = 43.4732,
        longitude = -80.5388
    )

    private fun mapSpots(): List<StudySpotSummary> = listOf(
        loadHomeSoloSpot(),
        StudySpotSummary(
            id = "dc-library",
            name = "DC Library",
            badge = "Quiet",
            distanceMeters = 240,
            rating = 4.6,
            studyContextLabel = "Silent floor",
            latitude = 43.4728,
            longitude = -80.5424
        ),
        StudySpotSummary(
            id = "slc-great-hall",
            name = "SLC Great Hall",
            badge = "Lively",
            distanceMeters = 310,
            rating = 4.2,
            studyContextLabel = "Group-friendly",
            latitude = 43.4717,
            longitude = -80.5453
        ),
        StudySpotSummary(
            id = "dp-library",
            name = "Dana Porter Library",
            badge = "Quiet",
            distanceMeters = 520,
            rating = 4.5,
            studyContextLabel = "Silent floors",
            latitude = 43.4690,
            longitude = -80.5424
        ),
        StudySpotSummary(
            id = "qnc-atrium",
            name = "QNC Atrium",
            badge = "Moderate",
            distanceMeters = 430,
            rating = 4.3,
            studyContextLabel = "Open seating",
            latitude = 43.4719,
            longitude = -80.5430
        ),
        StudySpotSummary(
            id = "m3-atrium",
            name = "M3 Atrium",
            badge = "Moderate",
            distanceMeters = 600,
            rating = 4.1,
            studyContextLabel = "Open seating",
            latitude = 43.4736,
            longitude = -80.5408
        )
    )

    private fun defaultGroupSession() =
        GroupStudySession(
            id = "app-etizers-cs341",
            title = "app-etizers study sesh",
            subtitle = "CS 341 finals prep",
            proximityLabel = "all within 10 min",
            members = listOf(
                GroupMember("you", "Vraj Patel", "VB"),
                GroupMember("akshat", "Akshat J.", "AJ"),
                GroupMember("eric", "Eric Z.", "EZ"),
                GroupMember("raghav", "Raghav V.", "RV"),
                GroupMember("pavan", "Pavan J.", "PJ")
            )
        )

    private fun groupSpotCandidates() = listOf(
        StudySpotSummary(
            id = "slc-boardroom-2a",
            name = "SLC Boardroom 2A",
            badge = "Best fit",
            features = listOf(
                SpotFeature("Seats 8", SpotFeatureType.Seating),
                SpotFeature("Whiteboard", SpotFeatureType.Whiteboard),
                SpotFeature("Fast Wi-Fi", SpotFeatureType.Wifi),
                SpotFeature("Accessible", SpotFeatureType.Accessible)
            ),
            bestFit = true
        ),
        StudySpotSummary(
            id = "e5-collaboration-lab",
            name = "E5 Collaboration Lab",
            badge = "Moderate",
            features = listOf(
                SpotFeature("Seats 6", SpotFeatureType.Seating),
                SpotFeature("Outlets", SpotFeatureType.Outlets),
                SpotFeature("Moderate noise", SpotFeatureType.Noise)
            )
        ),
        StudySpotSummary(
            id = "dc-team-room-4",
            name = "DC Team Room 4",
            badge = "Good",
            features = listOf(
                SpotFeature("Seats 10", SpotFeatureType.Seating),
                SpotFeature("Projector", SpotFeatureType.Projector),
                SpotFeature("Whiteboard", SpotFeatureType.Whiteboard),
                SpotFeature("Near cafe", SpotFeatureType.NearbyCafe)
            )
        )
    )

    private fun checkedInStudentsFor(
        mode: StudyMode,
        groupMembers: List<GroupMember>
    ): List<CheckedInStudent> =
        when (mode) {
            StudyMode.Solo -> soloCheckInStudents()
            StudyMode.Group -> groupCheckInStudents(groupMembers)
        }

    private fun soloCheckInStudents() = listOf(
        checkedInSelf(),
        CheckedInStudent("akshat", "AJ", "Akshat J.", "ECE 222 - 45 min here"),
        CheckedInStudent("eric", "EZ", "Eric Z.", "MATH 237 - 20 min here"),
        CheckedInStudent("raghav", "RV", "Raghav V.", "ECE 298 - 1h here", isFriend = true),
        CheckedInStudent("mei", "ML", "Mei L.", "CS 350 - 12 min here"),
        CheckedInStudent("nora", "NK", "Nora K.", "STAT 231 - 1h 08 min here"),
        CheckedInStudent("sam", "SC", "Sam C.", "PHYS 122 - 32 min here"),
        CheckedInStudent("tina", "TP", "Tina P.", "MATH 239 - 6 min here"),
        CheckedInStudent("omar", "OH", "Omar H.", "CS 246 - 51 min here"),
        CheckedInStudent("julia", "JW", "Julia W.", "ECE 250 - 24 min here"),
        CheckedInStudent("dev", "DS", "Dev S.", "ECON 101 - 15 min here")
    )

    private fun groupCheckInStudents(groupMembers: List<GroupMember>): List<CheckedInStudent> {
        val memberRows = groupMembers.mapIndexed { index, member ->
            if (member.id == "you") {
                checkedInSelf()
            } else {
                CheckedInStudent(
                    id = member.id,
                    initials = member.initials,
                    name = member.name,
                    detail = if (index < 5) {
                        "CS 341 group session - ${10 + index * 7} min here"
                    } else {
                        "Invited to this session"
                    },
                    isFriend = index < 5
                )
            }
        }

        return memberRows + listOf(
            CheckedInStudent("edmond", "EY", "Edmond Y.", "CS 348 nearby group - 28 min here"),
            CheckedInStudent("maya", "MR", "Maya R.", "SE 212 whiteboard session - 18 min here"),
            CheckedInStudent("leah", "LM", "Leah M.", "BIOL 130 exam review - 10 min here"),
            CheckedInStudent("kai", "KC", "Kai C.", "ME 269 group notes - 55 min here"),
            CheckedInStudent("priya", "PN", "Priya N.", "CO 250 tutorial prep - 7 min here"),
            CheckedInStudent("ben", "BT", "Ben T.", "ECE 106 group quiz - 41 min here"),
            CheckedInStudent("lina", "LZ", "Lina Z.", "CS 240 review - 22 min here")
        )
    }

    private fun checkedInSelf() = CheckedInStudent(
        id = "you",
        initials = "VB",
        name = "You (Vraj)",
        detail = "CS 341 - studying now",
        isSelf = true
    )
}
