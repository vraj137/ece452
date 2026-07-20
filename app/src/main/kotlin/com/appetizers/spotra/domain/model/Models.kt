package com.appetizers.spotra.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StudyTerm(val label: String) {
    @SerialName("1A") ONE_A("1A"),
    @SerialName("1B") ONE_B("1B"),
    @SerialName("2A") TWO_A("2A"),
    @SerialName("2B") TWO_B("2B"),
    @SerialName("3A") THREE_A("3A"),
    @SerialName("3B") THREE_B("3B"),
    @SerialName("4A") FOUR_A("4A"),
    @SerialName("4B") FOUR_B("4B"),
    @SerialName("Graduate") GRADUATE("Graduate"),
    @SerialName("Other") OTHER("Other")
}

data class UserProfile(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val program: String = "",
    val studyTerm: StudyTerm? = null,
    val onboardingComplete: Boolean = true,
    val loginStreak: Int = 0,
    val longestLoginStreak: Int = 0,
    val checkoutCount: Int = 0,
)

/** A peer shown in the social directory. Profiles are intentionally limited to
 * name, program, and term; email addresses are never exposed to other users. */
data class SocialUser(
    val id: String,
    val name: String,
    val program: String,
    val studyTerm: StudyTerm
) {
    val initials: String
        get() = name.split(" ").filter(String::isNotBlank).take(2)
            .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
}

data class SocialSnapshot(
    val friends: List<SocialUser> = emptyList(),
    val incomingRequests: List<SocialUser> = emptyList(),
    val suggestedUsers: List<SocialUser> = emptyList(),
    val outgoingRequestIds: Set<String> = emptySet()
)
enum class BadgeId(val label: String, val description: String) {
    LOGIN_STREAK_2("2-Day Streak", "Opened the app 2 days running"),
    LOGIN_STREAK_5("5-Day Streak", "Opened the app 5 days running"),
    LOGIN_STREAK_14("2-Week Streak", "Opened the app 14 days running"),
    FIRST_CHECKOUT("First Session", "Completed your first study session"),
    SESSION_VETERAN("Study Veteran", "Completed 10 study sessions"),
    FIRST_REVIEW("First Review", "Wrote your first spot review"),
    QUALITY_REVIEWER("Helpful Reviewer", "Wrote a detailed, quality review"),
    PROLIFIC_REVIEWER("Prolific Reviewer", "Wrote 5+ quality reviews"),
}

data class UserBadge(val id: BadgeId, val earnedAtMillis: Long)

@Serializable
data class OnboardingDraft(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val program: String = "",
    val studyTerm: StudyTerm? = null
)
