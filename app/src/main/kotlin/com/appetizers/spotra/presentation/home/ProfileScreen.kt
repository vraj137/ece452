package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.CompletedSession
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class RecentSession(
    val spotName: String,
    val timeLabel: String,
    val duration: String
)

private fun CompletedSession.toRecentSession() = RecentSession(
    spotName = spotName,
    timeLabel = timeLabel(),
    duration = durationSeconds.toDurationLabel()
)

private fun CompletedSession.timeLabel(): String {
    val elapsed = System.currentTimeMillis() - finishedAtMillis
    val hours = elapsed / (1000L * 60 * 60)
    val days = hours / 24
    return when {
        hours < 1 -> "Just now"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> "Last week"
    }
}

private fun Int.toDurationLabel(): String {
    val minutes = this / 60
    return when {
        minutes < 1 -> "< 1 min"
        minutes < 60 -> "${minutes}min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}min"
    }
}

enum class LocationVisibility(val databaseValue: String) {
    Everyone("everyone"),
    Visible("visible"),
    Approximate("approximate"),
    Hidden("hidden");

    val label get() = when (this) {
        Everyone -> "Visible to everyone"
        Visible -> "Visible to friends"
        Approximate -> "Approximate only"
        Hidden -> "Hidden"
    }
    val description get() = when (this) {
        Everyone -> "Anyone signed in can see your active checked-in spot"
        Visible -> "Friends can see your active checked-in spot"
        Approximate -> "Friends see the building, not the exact spot"
        Hidden -> "Your checked-in spot stays private"
    }

    companion object {
        fun fromDatabaseValue(value: String?): LocationVisibility = when (value) {
            "everyone" -> Everyone
            "visible" -> Visible
            "approximate" -> Approximate
            else -> Hidden
        }
    }
}

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val friendRepository: FriendRepository?,
    private val badgeRepository: BadgeRepository,
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val profile: UserProfile? = null,
        val friends: List<FriendProfile> = emptyList(),
        val badges: List<UserBadge> = emptyList(),
        val locationVisibility: LocationVisibility = LocationVisibility.Hidden,
        val isAcademicProfileSaving: Boolean = false,
        val academicProfileError: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val profileSaveMutex = Mutex()

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser()
            val profile = user?.let {
                runCatching { profileRepository.getProfile(it.id) }.getOrNull()
            }
            val friends = if (friendRepository != null) {
                runCatching { friendRepository.fetchFriendProfiles() }
                    .getOrDefault(emptyList())
                    .filter { it.isAccepted }
            } else emptyList()
            val badges = user?.let {
                runCatching { badgeRepository.getBadges(it.id) }.getOrDefault(emptyList())
            } ?: emptyList()
            val restoredVisibility = LocationVisibility.fromDatabaseValue(profile?.locationVisibility)
            _state.value = State(
                isLoading = false,
                profile = profile,
                friends = friends,
                badges = badges,
                locationVisibility = restoredVisibility,
            )
        }
    }

    fun refreshBadges() {
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.id ?: return@launch
            val badges = runCatching { badgeRepository.getBadges(userId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(badges = badges)
        }
    }

    /**
     * Friends are otherwise loaded once in [init], so accepting a request or unfriending someone
     * over in Social would leave this screen's avatar row stale until the app restarted.
     */
    fun refreshFriends() {
        val repository = friendRepository ?: return
        viewModelScope.launch {
            val friends = runCatching { repository.fetchFriendProfiles() }
                .getOrDefault(emptyList())
                .filter { it.isAccepted }
            _state.value = _state.value.copy(friends = friends)
        }
    }

    fun setLocationVisibility(visibility: LocationVisibility) {
        val previousProfile = _state.value.profile
        val previousVisibility = _state.value.locationVisibility
        val updatedProfile = _state.value.profile?.copy(
            locationVisibility = visibility.databaseValue
        )
        _state.value = _state.value.copy(
            profile = updatedProfile,
            locationVisibility = visibility,
            academicProfileError = null,
        )
        viewModelScope.launch {
            var attemptedVisibility = visibility
            runCatching {
                profileSaveMutex.withLock {
                    val latestProfile = _state.value.profile ?: return@withLock
                    attemptedVisibility = _state.value.locationVisibility
                    profileRepository.updateLocationVisibility(
                        userId = latestProfile.userId,
                        visibility = attemptedVisibility.databaseValue,
                    )
                }
            }.onSuccess {
                _state.value = _state.value.copy(academicProfileError = null)
            }.onFailure { error ->
                val authoritativeProfile = previousProfile?.let { profile ->
                    runCatching { profileRepository.getProfile(profile.userId) }.getOrNull()
                }
                _state.value = _state.value.let { current ->
                    if (current.locationVisibility != attemptedVisibility) {
                        current.copy(
                            academicProfileError = error.toUserMessage("Could not update location visibility.")
                        )
                    } else {
                        val restoredProfile = authoritativeProfile ?: previousProfile
                        val restoredVisibility = authoritativeProfile
                            ?.let { LocationVisibility.fromDatabaseValue(it.locationVisibility) }
                            ?: previousVisibility
                        current.copy(
                            profile = restoredProfile,
                            locationVisibility = restoredVisibility,
                            academicProfileError = error.toUserMessage("Could not update location visibility."),
                        )
                    }
                }
            }
        }
    }

    fun saveAcademicProfile(
        program: String,
        studyTerm: StudyTerm?,
        onSuccess: () -> Unit = {},
    ) {
        val cleanProgram = program.trim()
        if (cleanProgram.isNotEmpty() && cleanProgram.length < 2) {
            _state.value = _state.value.copy(
                academicProfileError = "Enter at least 2 characters for your major, or leave it blank."
            )
            return
        }
        if (_state.value.isAcademicProfileSaving) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isAcademicProfileSaving = true,
                academicProfileError = null,
            )
            runCatching {
                profileSaveMutex.withLock {
                    val currentProfile = requireNotNull(_state.value.profile) {
                        "Your profile is still loading."
                    }
                    val updatedProfile = currentProfile.copy(
                        program = cleanProgram,
                        studyTerm = studyTerm,
                    )
                    profileRepository.saveProfile(updatedProfile)
                    updatedProfile
                }
            }.onSuccess { updatedProfile ->
                _state.value = _state.value.copy(
                    profile = updatedProfile,
                    isAcademicProfileSaving = false,
                    academicProfileError = null,
                )
                onSuccess()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isAcademicProfileSaving = false,
                    academicProfileError = error.toUserMessage("Could not save your academic profile."),
                )
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
            onDone()
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val authRepository: AuthRepository,
        private val friendRepository: FriendRepository?,
        private val badgeRepository: BadgeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(profileRepository, authRepository, friendRepository, badgeRepository) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ProfileTabContent(
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    badgeRepository: BadgeRepository,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    friendRepository: FriendRepository? = null,
    recentSessions: List<CompletedSession> = emptyList(),
) {
    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(profileRepository, authRepository, friendRepository, badgeRepository)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.refreshBadges()
        vm.refreshFriends()
    }
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        ProfileHeader(profile = state.profile, onSettingsClick = { showSettings = true })
        val displaySessions = recentSessions.map { it.toRecentSession() }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileStats(sessions = recentSessions) }
            item { BadgesSection(badges = state.badges) }
            if (state.friends.isNotEmpty() || friendRepository != null) {
                item { FriendsSection(friends = state.friends) }
            }
            item { RecentSessionsHeader() }
            items(displaySessions) { session ->
                RecentSessionRow(session)
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            var program by remember(state.profile?.userId) {
                mutableStateOf(state.profile?.program.orEmpty())
            }
            var studyTerm by remember(state.profile?.userId) {
                mutableStateOf(state.profile?.studyTerm)
            }
            val programIsValid = program.isBlank() || program.trim().length >= 2
            val academicProfileChanged =
                program.trim() != state.profile?.program.orEmpty() ||
                    studyTerm != state.profile?.studyTerm

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 16.dp),
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                HorizontalDivider(color = DividerLine)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ACADEMIC PROFILE",
                    modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
                    color = SectionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                AcademicProgramField(
                    value = program,
                    onValueChange = {
                        if (it.length <= 80) program = it
                    },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Year of study",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AcademicTermChip(
                        label = "Not specified",
                        selected = studyTerm == null,
                        onClick = { studyTerm = null },
                    )
                    StudyTerm.entries.forEach { term ->
                        AcademicTermChip(
                            label = term.label,
                            selected = studyTerm == term,
                            onClick = { studyTerm = term },
                        )
                    }
                }
                if (!programIsValid) {
                    Text(
                        text = "Enter at least 2 characters, or leave the major blank.",
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp),
                        color = Color(0xFFD83D3C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                state.academicProfileError?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp),
                        color = Color(0xFFD83D3C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        vm.saveAcademicProfile(program, studyTerm) {
                            showSettings = false
                        }
                    },
                    enabled = programIsValid &&
                        academicProfileChanged &&
                        !state.isAcademicProfileSaving &&
                        state.profile != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoloBlue,
                        disabledContainerColor = SwitcherTrack,
                        disabledContentColor = HeaderMuted,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.isAcademicProfileSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = if (state.isAcademicProfileSaving) "Saving..." else "Save academic profile",
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = DividerLine)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "LOCATION VISIBILITY",
                    modifier = Modifier.padding(start = 24.dp, bottom = 10.dp),
                    color = SectionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                LocationVisibility.entries.forEach { option ->
                    PrivacyOptionRow(
                        option = option,
                        selected = state.locationVisibility == option,
                        onClick = { vm.setLocationVisibility(option) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = DividerLine)
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.ExitToApp,
                    label = "Sign out",
                    tint = Color(0xFFD83D3C),
                    onClick = {
                        showSettings = false
                        vm.signOut(onSignOut)
                    }
                )
            }
        }
    }
}

@Composable
private fun AcademicProgramField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = "Major / program",
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(HomeBackground, RoundedCornerShape(12.dp))
                .border(1.dp, DividerLine, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "e.g. Computer Engineering",
                    color = HeaderMuted,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(SoloBlue),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AcademicTermChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .background(
                if (selected) SoloBlue.copy(alpha = 0.12f) else HomeBackground,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (selected) SoloBlue else DividerLine,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        color = if (selected) SoloBlue else BodyText,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
    )
}

@Composable
private fun ProfileHeader(profile: UserProfile?, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "sp", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "o", color = SoloBlue, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "tra", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(HomeBackground, RoundedCornerShape(12.dp))
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = HeaderMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        val firstName = profile?.firstName ?: ""
        val lastName = profile?.lastName ?: ""
        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty()) append(lastName.first().uppercaseChar())
        }.ifBlank { "?" }

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(SoloBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(14.dp))

        if (profile != null) {
            Text(
                text = "${profile.firstName} ${profile.lastName}",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = profile.email,
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.program.isNotBlank()) ProfileTag(label = profile.program)
                profile.studyTerm?.let { ProfileTag(label = it.label) }
            }
        } else {
            Text(
                text = "Loading profile...",
                color = HeaderMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProfileTag(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .background(SwitcherTrack, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        color = BodyText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ProfileStats(sessions: List<CompletedSession>) {
    val totalMinutes = sessions.sumOf { it.durationSeconds } / 60
    val totalDisplay = when {
        totalMinutes >= 60 -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
        totalMinutes > 0 -> "${totalMinutes}m"
        else -> "0m"
    }
    val uniqueSpots = sessions.map { it.spotName }.distinct().size
    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val thisWeek = sessions.count { it.finishedAtMillis >= weekAgo }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(value = totalDisplay, label = "STUDIED", modifier = Modifier.weight(1f))
        StatCard(value = "$uniqueSpots", label = "SPOTS", modifier = Modifier.weight(1f))
        StatCard(value = "$thisWeek", label = "THIS WEEK", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = SoloBlue,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = SectionLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RecentSessionsHeader() {
    Text(
        text = "RECENTLY STUDIED AT",
        modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 10.dp, end = 22.dp),
        color = SectionLabel,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun RecentSessionRow(session: RecentSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(CardBackground, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = SoloBlue,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.spotName,
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = session.timeLabel,
                color = HeaderMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = session.duration,
            color = BodyText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 78.dp)
            .background(DividerLine)
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = label, color = tint, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrivacyOptionRow(
    option: LocationVisibility,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (selected) SoloBlue else Color.Transparent,
                    androidx.compose.foundation.shape.CircleShape
                )
                .border(
                    2.dp,
                    if (selected) SoloBlue else DividerLine,
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = option.label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = option.description, color = HeaderMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BadgesSection(badges: List<UserBadge>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "BADGES",
            color = SectionLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        if (badges.isEmpty()) {
            Text(
                text = "Complete study sessions and write reviews to earn badges.",
                color = HeaderMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(badges) { badge -> BadgePill(badge) }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 20.dp).background(DividerLine))
}

@Composable
private fun BadgePill(badge: UserBadge) {
    val (icon, tint) = when (badge.id) {
        BadgeId.LOGIN_STREAK_2, BadgeId.LOGIN_STREAK_5, BadgeId.LOGIN_STREAK_14 ->
            Icons.Rounded.Bolt to StarGold
        BadgeId.FIRST_CHECKOUT, BadgeId.SESSION_VETERAN ->
            Icons.Rounded.School to SoloBlue
        BadgeId.FIRST_REVIEW, BadgeId.QUALITY_REVIEWER, BadgeId.PROLIFIC_REVIEWER ->
            Icons.Rounded.Star to Color(0xFF4CAF50)
    }
    Row(
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), RoundedCornerShape(50.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(badge.id.label, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun FriendsSection(friends: List<FriendProfile>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "FRIENDS (${friends.size})",
            color = SectionLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        if (friends.isEmpty()) {
            Text(
                text = "No friends yet. Find people on the Social tab.",
                color = HeaderMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(friends) { friend ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(avatarColorFor(friend.id), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.initials,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = friend.firstName,
                            color = BodyText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 20.dp).background(DividerLine))
}
