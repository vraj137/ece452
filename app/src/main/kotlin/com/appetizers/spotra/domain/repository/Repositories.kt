package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.EmailInviteResult
import com.appetizers.spotra.domain.model.GroupInvite
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.GroupSessionEvent
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotOccupancy
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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

    suspend fun updateLocationVisibility(userId: String, visibility: String) {
        val current = requireNotNull(getProfile(userId)) { "Profile not found." }
        saveProfile(current.copy(locationVisibility = visibility))
    }
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
    suspend fun update(reviewId: String, draft: ReviewDraft)
    suspend fun delete(reviewId: String)
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
    fun observeOccupancy(): Flow<SpotOccupancy> = emptyFlow()
    suspend fun loadSharedLocationCounts(): Map<String, Int> = emptyMap()
    fun observeGroupSession(groupSessionId: String): Flow<GroupSessionEvent> = emptyFlow()
    suspend fun loadHome(): HomeSnapshot
    suspend fun fetchActiveCheckIn(): Pair<CheckInSession, Long>? = null
    suspend fun spotDetail(spotId: String): StudySpotDetail
    suspend fun childSpots(parentSpotId: String): List<StudySpotDetail>
    suspend fun createGroup(title: String, visibility: GroupVisibility): GroupStudySession
    suspend fun joinPublicGroup(groupSessionId: String): GroupStudySession
    suspend fun leaveGroup(groupSessionId: String)
    suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?,
    ): CheckInSession
    suspend fun checkOut(sessionId: String)
    suspend fun inviteToGroup(groupSessionId: String, inviteText: String): EmailInviteResult
    suspend fun inviteToGroupByUserId(groupSessionId: String, userId: String): GroupMember
    fun observePublicGroups(): Flow<Unit> = emptyFlow()
    fun observeGroupInvites(currentUserId: String): Flow<Unit> = emptyFlow()
    suspend fun loadPublicGroupsSnapshot(excludingId: String?): List<GroupStudySession> = emptyList()
    suspend fun loadCheckInAttendees(spotSlug: String): List<CheckedInStudent> = emptyList()
    suspend fun fetchPendingGroupInvites(): List<GroupInvite> = emptyList()
    suspend fun respondToGroupInvite(inviteId: String, accept: Boolean)
    suspend fun fetchGroupSessionMembers(groupSessionId: String): List<GroupMember> = emptyList()
}
