package com.appetizers.spotra.domain.repository

import com.appetizers.spotra.domain.model.OnboardingDraft
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
