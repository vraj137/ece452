package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.HomeSnapshot
import com.appetizers.spotra.domain.model.OnboardingDraft
import com.appetizers.spotra.domain.model.StudyMode
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

interface HomeRepository {
    suspend fun loadHome(): HomeSnapshot
    suspend fun startCheckIn(
        spotId: String,
        mode: StudyMode,
        groupSessionId: String?
    ): CheckInSession
    suspend fun checkOut(sessionId: String)
    suspend fun sendBuddyRequest(studentId: String)
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
