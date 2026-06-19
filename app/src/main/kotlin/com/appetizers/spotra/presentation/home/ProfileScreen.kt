package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
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

private val mockRecentSessions = listOf(
    RecentSession("E7 Study Hall", "Yesterday", "2h 15min"),
    RecentSession("DC Library 3F", "2 days ago", "1h 30min"),
    RecentSession("SLC Boardroom 2A", "3 days ago", "45min"),
    RecentSession("MC Atrium", "5 days ago", "2h 00min"),
    RecentSession("DP Library", "Last week", "1h 10min")
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val profile: UserProfile? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = authRepository.currentUser()
            val profile = user?.let {
                runCatching { profileRepository.getProfile(it.id) }.getOrNull()
            }
            _state.value = State(isLoading = false, profile = profile)
        }
    }

    class Factory(
        private val profileRepository: ProfileRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(profileRepository, authRepository) as T
    }
}

@Composable
internal fun ProfileTabContent(
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(profileRepository, authRepository)
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        ProfileHeader(state.profile)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ProfileStats() }
            item { RecentSessionsHeader() }
            items(mockRecentSessions) { session ->
                RecentSessionRow(session)
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: UserProfile?) {
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
                ProfileTag(label = profile.program)
                ProfileTag(label = profile.studyTerm.label)
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
