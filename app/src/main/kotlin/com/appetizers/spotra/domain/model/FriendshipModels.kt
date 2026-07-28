package com.appetizers.spotra.domain.model

data class FriendProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String = "",
    val program: String = "",
    val term: String = "",
    val friendshipId: String? = null,
    val friendshipStatus: FriendshipStatus? = null,
    val isRequester: Boolean? = null,
    val streakDays: Int? = null,
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val initials: String get() = buildString {
        firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }.ifBlank { "?" }
    val displayDetail: String get() = when {
        program.isNotBlank() && term.isNotBlank() -> "$term · $program"
        program.isNotBlank() -> program
        term.isNotBlank() -> term
        else -> "UWaterloo student"
    }
    val isAccepted: Boolean get() = friendshipStatus == FriendshipStatus.ACCEPTED
    val isPendingIncoming: Boolean get() = friendshipStatus == FriendshipStatus.PENDING && isRequester == false
    val isPendingOutgoing: Boolean get() = friendshipStatus == FriendshipStatus.PENDING && isRequester == true
}

enum class FriendshipStatus { PENDING, ACCEPTED, DECLINED }
