package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import com.appetizers.spotra.domain.repository.StreakRepository

private const val MISSING_SUPABASE_CONFIGURATION =
    "Live data is unavailable because Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY."

private fun missingSupabaseConfiguration(): Nothing = error(MISSING_SUPABASE_CONFIGURATION)

class MissingConfigurationHomeRepository : HomeRepository {
    override suspend fun loadHome(): HomeSnapshot = missingSupabaseConfiguration()
    override suspend fun spotDetail(spotId: String): StudySpotDetail = missingSupabaseConfiguration()
    override suspend fun childSpots(parentSpotId: String): List<StudySpotDetail> = missingSupabaseConfiguration()
    override suspend fun createGroup(
        title: String,
        visibility: GroupVisibility
    ): GroupStudySession = missingSupabaseConfiguration()
    override suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession =
        missingSupabaseConfiguration()
    override suspend fun leaveGroup(groupSessionId: String) = missingSupabaseConfiguration()
    override suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession = missingSupabaseConfiguration()
    override suspend fun checkOut(sessionId: String) = missingSupabaseConfiguration()
    override suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember =
        missingSupabaseConfiguration()
}

class MissingConfigurationSpotSubmissionRepository : SpotSubmissionRepository {
    override suspend fun submitSpot(submission: SpotSubmission) = missingSupabaseConfiguration()
}

class MissingConfigurationReviewRepository : ReviewRepository {
    override suspend fun reviewsFor(spotSlug: String): List<Review> = missingSupabaseConfiguration()
    override suspend fun submit(draft: ReviewDraft) = missingSupabaseConfiguration()
    override suspend fun getReviewCount(userId: String): Int = missingSupabaseConfiguration()
    override suspend fun getQualityReviewCount(userId: String): Int = missingSupabaseConfiguration()
}

class MissingConfigurationFriendRepository : FriendRepository {
    override suspend fun currentUserId(): String? = missingSupabaseConfiguration()
    override suspend fun fetchFriendProfiles(): List<FriendProfile> = missingSupabaseConfiguration()
    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> =
        missingSupabaseConfiguration()
    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> =
        missingSupabaseConfiguration()
    override suspend fun sendRequest(toUserId: String) = missingSupabaseConfiguration()
    override suspend fun acceptRequest(friendshipId: String) = missingSupabaseConfiguration()
    override suspend fun declineRequest(friendshipId: String) = missingSupabaseConfiguration()
    override suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile> =
        missingSupabaseConfiguration()
}

class MissingConfigurationStreakRepository : StreakRepository {
    override suspend fun recordLogin(userId: String): Int = missingSupabaseConfiguration()
    override suspend fun recordCheckout(
        userId: String,
        spotId: String,
        spotName: String,
        durationSeconds: Int
    ): Int = missingSupabaseConfiguration()
    override suspend fun fetchRecentSessions(userId: String): List<CompletedSession> =
        missingSupabaseConfiguration()
}

class MissingConfigurationBadgeRepository : BadgeRepository {
    override suspend fun getBadges(userId: String): List<UserBadge> = missingSupabaseConfiguration()
    override suspend fun awardBadge(userId: String, badgeId: BadgeId) = missingSupabaseConfiguration()
}
