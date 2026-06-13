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
    @SerialName("program") val program: String,
    @SerialName("study_term") val studyTerm: StudyTerm,
    @SerialName("onboarding_complete") val onboardingComplete: Boolean = true
) {
    fun toDomain() = UserProfile(
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        program = program,
        studyTerm = studyTerm,
        onboardingComplete = onboardingComplete
    )
}

fun UserProfile.toDto() = UserProfileDto(
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    email = email,
    program = program,
    studyTerm = studyTerm,
    onboardingComplete = onboardingComplete
)
