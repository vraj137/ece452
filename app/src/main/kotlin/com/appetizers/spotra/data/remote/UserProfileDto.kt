package com.appetizers.spotra.data.remote

import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    @SerialName("id") val userId: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("email") val email: String,
    @SerialName("program") val program: String = "",
    @SerialName("study_term") val studyTerm: StudyTerm? = null,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean = true,
    @SerialName("login_streak") val loginStreak: Int = 0,
    @SerialName("longest_login_streak") val longestLoginStreak: Int = 0,
    @SerialName("checkout_count") val checkoutCount: Int = 0,
) {
    fun toDomain() = UserProfile(
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        program = program,
        studyTerm = studyTerm,
        onboardingComplete = onboardingComplete,
        loginStreak = loginStreak,
        longestLoginStreak = longestLoginStreak,
        checkoutCount = checkoutCount,
    )
}

fun UserProfile.toDto() = UserProfileDto(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    email = email,
    program = program,
    studyTerm = studyTerm,
    onboardingComplete = onboardingComplete,
    loginStreak = loginStreak,
    longestLoginStreak = longestLoginStreak,
    checkoutCount = checkoutCount,
)
