package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.model.SocialSnapshot
import kotlinx.coroutines.flow.Flow

data class AuthUser(val id: String, val email: String)

interface AuthRepository {
    suspend fun currentUser(): AuthUser?
    suspend fun sendOtp(email: String, createUser: Boolean)
    suspend fun verifyOtp(email: String, token: String): AuthUser
    suspend fun signOut()
}

interface ProfileRepository {
    suspend fun getProfile(userId: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
}

interface OnboardingDraftRepository {
    val draft: Flow<OnboardingDraft>
    suspend fun save(draft: OnboardingDraft)
    suspend fun clear()
}

interface SpotSubmissionRepository {
    suspend fun submitSpot(submission: SpotSubmission)
}

interface ReviewRepository {
    suspend fun reviewsFor(spotSlug: String): List<Review>
    suspend fun submit(draft: ReviewDraft)
    suspend fun getReviewCount(userId: String): Int
    suspend fun getQualityReviewCount(userId: String): Int
}

interface StreakRepository {
    suspend fun recordLogin(userId: String): Int
    suspend fun recordCheckout(userId: String, spotId: String, spotName: String, durationSeconds: Int): Int
    suspend fun fetchRecentSessions(userId: String): List<CompletedSession>
}

interface BadgeRepository {
    suspend fun getBadges(userId: String): List<UserBadge>
    suspend fun awardBadge(userId: String, badgeId: BadgeId)
}

interface HomeRepository {
    suspend fun loadHome(): HomeSnapshot
    suspend fun spotDetail(spotId: String): StudySpotDetail
    suspend fun childSpots(parentSpotId: String): List<StudySpotDetail>
    suspend fun createGroup(title: String, visibility: GroupVisibility): GroupStudySession
    suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession
    suspend fun leaveGroup(groupSessionId: String)
    suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession
    suspend fun checkOut(sessionId: String)
    suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember
}

interface SocialRepository {
    suspend fun loadSocial(): SocialSnapshot
    suspend fun sendFriendRequest(recipientId: String)
    suspend fun acceptFriendRequest(requesterId: String)
}

object EmptySocialRepository : SocialRepository {
    override suspend fun loadSocial() = SocialSnapshot()
    override suspend fun sendFriendRequest(recipientId: String) = Unit
    override suspend fun acceptFriendRequest(requesterId: String) = Unit
}
