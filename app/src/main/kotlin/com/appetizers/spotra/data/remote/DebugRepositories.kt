package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.ProfileRepository

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
