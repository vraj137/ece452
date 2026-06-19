package com.appetizers.spotra.data.remote

import com.appetizers.spotra.data.mock.MockData
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository

// In-memory debug implementations — used when Supabase credentials are absent.
// All spot and user data is sourced from MockData so there is a single source
// of truth; do not hard-code names, IDs, or spot lists here.

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
    // Pre-seeded with the mock user so the Profile screen is populated in debug
    // builds even before onboarding is completed.  saveProfile() overwrites this
    // with the user's actual input, so the two flows do not conflict.
    private var profile: UserProfile? = MockData.user

    override suspend fun getProfile(userId: String): UserProfile? = profile

    override suspend fun saveProfile(profile: UserProfile) {
        this.profile = profile
    }
}

class DebugHomeRepository : HomeRepository {
    private var groupSession = MockData.groupSession

    override suspend fun loadHome(): HomeSnapshot = HomeSnapshot(
        userFirstName = MockData.user.firstName,
        soloSpot = MockData.spotById("e7-study-hall")!!.toSummary(),
        groupSession = groupSession,
        groupSpots = MockData.groupSpots.map { it.toSummary() },
        mapSpots = mapSpots()
    )

    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession {
        val spot = MockData.spotById(spotId)?.toSummary()
            ?: error("Unknown study spot: $spotId")
        return CheckInSession(
            id = "debug-$spotId-${mode.name}",
            spot = spot,
            mode = mode,
            attendees = attendeesFor(mode)
        )
    }

    override suspend fun checkOut(sessionId: String) = Unit

    override suspend fun sendBuddyRequest(studentId: String) = Unit

    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember {
        val initials = inviteText
            .split(" ")
            .filter(String::isNotBlank)
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
            .take(2)
        return GroupMember(
            id = "invite-${inviteText.lowercase().replace(" ", "-")}-${groupSession.members.size}",
            name = inviteText,
            initials = initials
        ).also { member ->
            groupSession = groupSession.copy(members = groupSession.members + member)
        }
    }

    private fun mapSpots(): List<StudySpotSummary> = MockData.spots.map { it.toSummary() }

    private fun attendeesFor(mode: StudyMode): List<CheckedInStudent> {
        val self = CheckedInStudent(
            id = "you",
            initials = MockData.selfInitials,
            name = "You (${MockData.user.firstName})",
            detail = "CS 341 - studying now",
            isSelf = true
        )
        return when (mode) {
            StudyMode.Solo -> buildList {
                add(self)
                MockData.soloCheckInStudents.forEach { (id, initials, name) ->
                    val isFriend = id == "raghav"
                    add(CheckedInStudent(id, initials, name, courseDetailFor(id), isFriend = isFriend))
                }
            }
            StudyMode.Group -> buildList {
                groupSession.members.mapIndexed { index, member ->
                    if (member.id == "you") {
                        add(self)
                    } else {
                        add(
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
                        )
                    }
                }
                add(CheckedInStudent("edmond", "EY", "Edmond Y.", "CS 348 nearby group - 28 min here"))
                add(CheckedInStudent("maya", "MR", "Maya R.", "SE 212 whiteboard session - 18 min here"))
                add(CheckedInStudent("leah", "LM", "Leah M.", "BIOL 130 exam review - 10 min here"))
            }
        }
    }

    private fun courseDetailFor(id: String): String = when (id) {
        "akshat" -> "ECE 222 - 45 min here"
        "eric"   -> "MATH 237 - 20 min here"
        "raghav" -> "ECE 298 - 1h here"
        "mei"    -> "CS 350 - 12 min here"
        "nora"   -> "STAT 231 - 1h 08 min here"
        "sam"    -> "PHYS 122 - 32 min here"
        "tina"   -> "MATH 239 - 6 min here"
        "omar"   -> "CS 246 - 51 min here"
        "julia"  -> "ECE 250 - 24 min here"
        "dev"    -> "ECON 101 - 15 min here"
        else     -> "Studying here"
    }
}
