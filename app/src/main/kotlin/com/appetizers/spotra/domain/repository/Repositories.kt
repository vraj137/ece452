package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.UserProfile
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
}

interface HomeRepository {
    suspend fun loadHome(): HomeSnapshot
    suspend fun spotDetail(spotId: String): StudySpotDetail
    suspend fun childSpots(parentSpotId: String): List<StudySpotDetail>
    suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession
    suspend fun checkOut(sessionId: String)
    suspend fun sendBuddyRequest(studentId: String)
    suspend fun inviteToGroup(groupSessionId: String, inviteText: String): GroupMember
}
