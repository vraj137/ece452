package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

private val mockRecentSessions = listOf(
    RecentSession("E7 Study Hall", "Yesterday", "2h 15min"),
    RecentSession("DC Library 3F", "2 days ago", "1h 30min"),
    RecentSession("SLC Boardroom 2A", "3 days ago", "45min"),
    RecentSession("MC Atrium", "5 days ago", "2h 00min"),
    RecentSession("DP Library", "Last week", "1h 10min")
)

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
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

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
            _state.value = State(isLoading = false, profile = profile, friends = friends, badges = badges)
        }
    }

    fun refreshBadges() {
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.id ?: return@launch
            val badges = runCatching { badgeRepository.getBadges(userId) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(badges = badges)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileTabContent(
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    friendRepository: FriendRepository? = null,
    badgeRepository: BadgeRepository,
    onSignOut: () -> Unit,
    recentSessions: List<CompletedSession> = emptyList(),
    modifier: Modifier = Modifier
) {
    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(profileRepository, authRepository, friendRepository, badgeRepository)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshBadges() }
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        ProfileHeader(profile = state.profile, onSettingsClick = { showSettings = true })
        val displaySessions = recentSessions.map { it.toRecentSession() } + mockRecentSessions
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileStats() }
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
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 16.dp),
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                HorizontalDivider(color = DividerLine)
                SettingsRow(
                    icon = Icons.Rounded.ExitToApp,
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

        val firstName = profile?.firstName ?: "—"
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
private fun ProfileStats() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(value = "24h", label = "STUDIED", modifier = Modifier.weight(1f))
        StatCard(value = "8", label = "SPOTS", modifier = Modifier.weight(1f))
        StatCard(value = "3", label = "THIS WEEK", modifier = Modifier.weight(1f))
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
                text = "No friends yet — find people on the Social tab.",
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
