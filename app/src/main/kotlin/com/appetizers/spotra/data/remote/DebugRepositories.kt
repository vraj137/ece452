package com.appetizers.spotra.data.remote

import com.appetizers.spotra.data.mock.MockData
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.SocialRepository
import com.appetizers.spotra.domain.model.SocialSnapshot
import com.appetizers.spotra.domain.model.SocialUser
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.StreakRepository

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
    private var groupSession = MockData.groupSession

    override suspend fun loadHome(): HomeSnapshot = HomeSnapshot(
        userFirstName = MockData.user.firstName,
        soloSpot = MockData.spotById("e7-study-hall")!!.toSummary(),
        groupSession = groupSession,
        groupSpots = MockData.groupSpots.map { it.toSummary() },
        mapSpots = mapSpots(),
        trendingCounts = MockData.spots.associate { it.id to it.checkInsThisWeek }
    )

    override suspend fun spotDetail(spotId: String): StudySpotDetail =
        MockData.spotById(spotId)?.toDetail()
            ?: error("Unknown study spot: $spotId")

    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = emptyList()

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

class DebugFriendRepository : FriendRepository {

    private val mockUsers = listOf(
        FriendProfile("u1", "Raghav", "Verma",      program = "Computer Engineering",  term = "3B"),
        FriendProfile("u2", "Pavan",  "Jayasinha",  program = "Software Engineering",  term = "4A"),
        FriendProfile("u3", "Eric",   "Zhu",        program = "Computer Science",       term = "2B"),
        FriendProfile("u4", "Edmond", "Yu",         program = "Computer Engineering",  term = "3A"),
        FriendProfile("u5", "Sarah",  "Kim",        program = "Systems Design Eng.",   term = "3B"),
        FriendProfile("u6", "Anika",  "Mehta",      program = "Computer Science",       term = "4B"),
        FriendProfile("u7", "James",  "Park",       program = "Software Engineering",  term = "2A"),
        FriendProfile("u8", "Priya",  "Nair",       program = "Computer Engineering",  term = "3A")
    )

    private val sentRequests = mutableSetOf<String>()

    override suspend fun currentUserId(): String? = "debug-self"

    override suspend fun fetchFriendProfiles(): List<FriendProfile> = emptyList()

    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return mockUsers.filter { user ->
            user.id !in excludeIds &&
            (user.firstName.lowercase().contains(q) ||
             user.lastName.lowercase().contains(q) ||
             user.email.lowercase().contains(q))
        }
    }

    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> =
        mockUsers.filter { it.id !in acceptedFriendIds }.take(4)

    override suspend fun sendRequest(toUserId: String) { sentRequests.add(toUserId) }
    override suspend fun acceptRequest(friendshipId: String) = Unit
    override suspend fun declineRequest(friendshipId: String) = Unit
    override suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile> = emptyList()
}

class DebugReviewRepository : ReviewRepository {
    // Seeded from MockData reviews; locally submitted reviews are kept in memory
    // so the debug build reflects new submissions without a backend.
    private val submitted = mutableListOf<Review>()

    override suspend fun reviewsFor(spotSlug: String): List<Review> {
        val seed = MockData.spotById(spotSlug)?.reviews.orEmpty().map { review ->
            Review(
                id = "mock-$spotSlug-${review.reviewerId}",
                spotSlug = spotSlug,
                reviewerName = review.reviewerName,
                reviewerInitials = review.reviewerInitials,
                reviewerId = review.reviewerId,
                rating = review.rating,
                comment = review.comment
            )
        }
        return submitted.filter { it.spotSlug == spotSlug } + seed
    }

    override suspend fun submit(draft: ReviewDraft) {
        submitted.add(
            0,
            Review(
                id = "local-${System.nanoTime()}",
                spotSlug = draft.spotSlug,
                reviewerName = if (draft.anonymous) "Anonymous" else "You",
                reviewerInitials = if (draft.anonymous) "?" else "YO",
                reviewerId = "you",
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

    override suspend fun getReviewCount(userId: String): Int = submitted.size

    override suspend fun getQualityReviewCount(userId: String): Int = 0
}

class DebugStreakRepository : StreakRepository {
    override suspend fun recordLogin(userId: String): Int = 0
    override suspend fun recordCheckout(userId: String, spotId: String, spotName: String, durationSeconds: Int): Int = 0
}

class DebugBadgeRepository : BadgeRepository {
    override suspend fun getBadges(userId: String): List<UserBadge> = emptyList()
    override suspend fun awardBadge(userId: String, badgeId: BadgeId) = Unit
}
