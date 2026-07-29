package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.repository.FriendRepository

private data class SocialPerson(
    val id: String,
    val initials: String,
    val name: String,
    val detail: String,
    val active: Boolean = true,
    val streakDays: Int? = null,
)

@Composable
internal fun SocialScreen(
    selectedTab: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    friendRepository: FriendRepository,
    loginStreak: Int = 0,
    activeSessionBar: (@Composable () -> Unit)? = null
) {
    val vm: SocialViewModel = viewModel(factory = SocialViewModel.Factory(friendRepository))
    val state by vm.state.collectAsStateWithLifecycle()
    var pendingRemoval by remember { mutableStateOf<FriendProfile?>(null) }
    LaunchedEffect(selectedTab) {
        if (selectedTab == SocialTab.Discover) {
            vm.loadAll()
        }
    }

    pendingRemoval?.let { friend ->
        RemoveFriendSheet(
            friendName = friend.fullName,
            onConfirm = {
                vm.removeFriend(friend)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 30.dp, top = 34.dp, end = 30.dp)
        ) {
            Text(text = "Social", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(22.dp))
            SocialTabs(selectedTab, onTabSelected)
        }

        if (state.isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 40.dp),
                color = SoloBlue
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 30.dp, top = 22.dp, end = 30.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    SocialTab.Friends -> {
                        if (state.friends.isEmpty()) {
                            item {
                                Text(
                                    text = "No friends yet. Search for people in Discover.",
                                    color = HeaderMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            item { SectionHeader("YOUR FRIENDS (${state.friends.size})") }
                            itemsIndexed(state.friends) { index, friend ->
                                SocialPersonRow(
                                    person = SocialPerson(
                                        id = friend.id,
                                        initials = friend.initials,
                                        name = friend.fullName,
                                        detail = friend.displayDetail,
                                        streakDays = friend.streakDays,
                                    ),
                                    actionLabel = "",
                                    actionEnabled = false,
                                    onAction = {},
                                    onRemove = { pendingRemoval = friend }
                                )
                            }
                        }
                        item { StudyStreakCard(streakCount = loginStreak) }
                    }
                    SocialTab.Requests -> {
                        val badge = state.incomingRequests.size
                        if (state.incomingRequests.isEmpty() && state.outgoingRequests.isEmpty()) {
                            item {
                                Text(
                                    text = "No pending requests.",
                                    color = HeaderMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            if (state.incomingRequests.isNotEmpty()) {
                                item { SectionHeader("INCOMING ($badge)") }
                                itemsIndexed(state.incomingRequests) { _, request ->
                                    IncomingRequestRow(
                                        profile = request,
                                        onAccept = { vm.acceptRequest(request) },
                                        onDecline = { vm.declineRequest(request) }
                                    )
                                }
                            }
                            if (state.outgoingRequests.isNotEmpty()) {
                                item { SectionHeader("SENT") }
                                itemsIndexed(state.outgoingRequests) { _, request ->
                                    SocialPersonRow(
                                        person = SocialPerson(
                                            id = request.id,
                                            initials = request.initials,
                                            name = request.fullName,
                                            detail = request.displayDetail
                                        ),
                                        actionLabel = "Cancel",
                                        actionEnabled = true,
                                        onAction = { vm.cancelRequest(request) }
                                    )
                                }
                            }
                        }
                    }
                    SocialTab.Discover -> {
                        item {
                            SearchBar(
                                query = state.searchQuery,
                                onQueryChange = vm::updateSearchQuery
                            )
                        }
                        if (state.searchQuery.isNotBlank()) {
                            if (state.searchResults.isEmpty()) {
                                item {
                                    Text(
                                        text = "No results for \"${state.searchQuery}\".",
                                        color = HeaderMuted,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                item { SectionHeader("RESULTS") }
                                itemsIndexed(state.searchResults) { _, profile ->
                                    SocialPersonRow(
                                        person = SocialPerson(
                                            id = profile.id,
                                            initials = profile.initials,
                                            name = profile.fullName,
                                            detail = profile.displayDetail
                                        ),
                                        actionLabel = "+ Add",
                                        actionEnabled = true,
                                        onAction = { vm.sendRequest(profile) }
                                    )
                                }
                            }
                        } else if (state.suggestions.isNotEmpty()) {
                            item { SectionHeader("SAME PROGRAM OR YEAR") }
                            itemsIndexed(state.suggestions) { _, profile ->
                                SocialPersonRow(
                                    person = SocialPerson(
                                        id = profile.id,
                                        initials = profile.initials,
                                        name = profile.fullName,
                                        detail = profile.displayDetail.ifBlank { "UWaterloo student" }
                                    ),
                                    actionLabel = "+ Add",
                                    actionEnabled = true,
                                    onAction = { vm.sendRequest(profile) }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "No same-program or same-year suggestions yet. Search by name to find friends.",
                                    color = HeaderMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        activeSessionBar?.invoke()
        BottomNavigationShell(
            accent = SoloBlue,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeBackground, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.PersonAdd,
            contentDescription = null,
            tint = HeaderMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Search by name or email", color = HeaderMuted, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                inner()
            },
            singleLine = true
        )
    }
}

@Composable
private fun IncomingRequestRow(
    profile: com.appetizers.spotra.domain.model.FriendProfile,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(avatarColorFor(profile.id), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.initials, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.fullName, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(profile.displayDetail, color = HeaderMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "✓",
            modifier = Modifier
                .background(SoloBlue, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(onClick = onAccept)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "✗",
            modifier = Modifier
                .background(HomeBackground, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(onClick = onDecline)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = DPAtriumRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SocialTabs(
    selectedTab: SocialTab,
    onTabSelected: (SocialTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(1.dp, DividerLine, RoundedCornerShape(0.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SocialTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val label = when (tab) {
                SocialTab.Friends -> "Friends"
                SocialTab.Requests -> "Requests"
                SocialTab.Discover -> "Discover"
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) SoloBlue else HeaderMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    Modifier
                        .height(3.dp)
                        .fillMaxWidth(.72f)
                        .background(if (selected) SoloBlue else Color.Transparent)
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(label: String) {
    Text(
        text = label,
        color = SectionLabel,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
private fun SocialPersonRow(
    person: SocialPerson,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (person.active) Color.White else Color(0xFFF8F8F6), RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(avatarColorFor(person.id), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = person.initials,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = person.name,
                color = if (person.active) Ink else HeaderMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = person.detail,
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        when {
            person.streakDays != null -> FriendStreakBadge(person.streakDays)
            actionLabel.isNotEmpty() -> Text(
                text = actionLabel,
                modifier = Modifier
                    .background(if (actionEnabled) BuddyPill else SwitcherTrack, RoundedCornerShape(18.dp))
                    .clickable(enabled = actionEnabled, onClick = onAction)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (actionEnabled) SoloBlue else HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        // Sits outside the when-block so a friend showing a streak badge still gets the control.
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Remove ${person.name} from your friends"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = HeaderMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoveFriendSheet(
    friendName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Remove $friendName?",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "You’ll stop seeing each other’s check-ins and streaks. " +
                    "Either of you can send a new request later.",
                color = BodyText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Keep")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModerateFitText)
                ) {
                    Text(text = "Remove", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FriendStreakBadge(streakDays: Int) {
    Row(
        modifier = Modifier
            .background(StarGold.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$streakDays day login streak"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = StarGold,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = "${streakDays}d",
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun StudyStreakCard(streakCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F3EA), RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color(0xFFFFF0CC), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = StarGold,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (streakCount > 0) "$streakCount day login streak" else "No streak yet",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (streakCount > 0) "Open the app tomorrow to keep it going." else "Come back tomorrow to start your streak!",
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
