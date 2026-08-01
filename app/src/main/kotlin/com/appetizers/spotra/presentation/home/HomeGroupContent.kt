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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.GroupInvite
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.StudySpotSummary

@Composable
internal fun GroupSetupContent(
    groupName: String,
    visibility: GroupVisibility,
    publicGroups: List<GroupStudySession>,
    pendingGroupInvites: List<GroupInvite>,
    isCreating: Boolean,
    onGroupNameChange: (String) -> Unit,
    onVisibilityChange: (GroupVisibility) -> Unit,
    onCreateGroup: () -> Unit,
    onJoinPublicGroup: (String) -> Unit,
    onAcceptInvite: (String) -> Unit,
    onDeclineInvite: (String) -> Unit,
    onBack: () -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
) {
    val cleanName = groupName.trim()
    val canCreate = cleanName.length >= 2 && !isCreating

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 20.dp)
        ) {
            GroupHeaderTopRow(onBack)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Create your study group",
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Create your own group or join an open one and meet new study partners.",
                color = BodyText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .border(1.dp, GroupCardBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(GroupSpotIconGreen, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = null,
                            tint = GroupGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Group name", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(
                                1.5.dp,
                                if (groupName.isNotBlank()) GroupGreen else DividerLine,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = groupName,
                            onValueChange = onGroupNameChange,
                            enabled = !isCreating,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Ink,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (groupName.isBlank()) {
                                    Text("e.g. CS 341 Finals Crew", color = HeaderMuted, fontSize = 16.sp)
                                }
                                inner()
                            }
                        )
                        Text(
                            text = "${groupName.length}/50",
                            color = HeaderMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (groupName.isNotEmpty() && cleanName.length < 2) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "Use at least 2 characters.",
                            color = ModerateFitText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Who can join?", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GroupVisibilityOption(
                            visibility = GroupVisibility.Private,
                            selected = visibility == GroupVisibility.Private,
                            enabled = !isCreating,
                            onClick = { onVisibilityChange(GroupVisibility.Private) },
                            modifier = Modifier.weight(1f)
                        )
                        GroupVisibilityOption(
                            visibility = GroupVisibility.Public,
                            selected = visibility == GroupVisibility.Public,
                            enabled = !isCreating,
                            onClick = { onVisibilityChange(GroupVisibility.Public) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onCreateGroup,
                        enabled = canCreate,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GroupGreen,
                            disabledContainerColor = SwitcherTrack,
                            disabledContentColor = HeaderMuted
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Create group", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            if (pendingGroupInvites.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(30.dp))
                    Text(
                        text = "Group invitations",
                        color = Ink,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Someone has invited you to study with them.",
                        color = BodyText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(14.dp))
                }
                items(
                    items = pendingGroupInvites,
                    key = { it.id }
                ) { invite ->
                    GroupInviteCard(
                        invite = invite,
                        onAccept = { onAcceptInvite(invite.id) },
                        onDecline = { onDeclineInvite(invite.id) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                Spacer(Modifier.height(30.dp))
                Text(
                    text = "Open public groups",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Join students who are open to meeting new study partners.",
                    color = BodyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
            }
            if (publicGroups.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, GroupCardBorder, RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "No public groups are open yet",
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Create the first one and other students will be able to join.",
                            color = BodyText,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(
                    items = publicGroups,
                    key = { it.id }
                ) { group ->
                    PublicGroupCard(
                        group = group,
                        enabled = !isCreating,
                        onJoin = { onJoinPublicGroup(group.id) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        BottomNavigationShell(
            accent = GroupGreen,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }
}

@Composable
private fun GroupVisibilityOption(
    visibility: GroupVisibility,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPublic = visibility == GroupVisibility.Public
    Column(
        modifier = modifier
            .background(
                if (selected) GroupBestFitBackground else Color.White,
                RoundedCornerShape(14.dp)
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) GroupGreen else DividerLine,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = if (isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (selected) GroupGreen else BodyText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = if (isPublic) "Public" else "Private",
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isPublic) "Anyone can join" else "Invite only",
            color = BodyText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PublicGroupCard(
    group: GroupStudySession,
    enabled: Boolean,
    onJoin: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, GroupCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(GroupSpotIconGreen, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = null,
                tint = GroupGreen,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = group.title,
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Public group • Open to everyone",
                color = BodyText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onJoin,
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GroupGreen,
                disabledContainerColor = SwitcherTrack
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Join", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GroupInviteCard(
    invite: GroupInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, GroupCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(GroupSpotIconGreen, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = GroupGreen,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = invite.groupTitle,
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Invited by ${invite.inviterName}",
                    color = BodyText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Decline", fontWeight = FontWeight.Bold, color = BodyText)
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GroupGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Join group", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupModeContent(
    groupSession: GroupStudySession,
    spots: List<StudySpotSummary>,
    inviteText: String,
    invitableFriends: List<FriendProfile>,
    invitedGroupFriendIds: Set<String> = emptySet(),
    isActionInProgress: Boolean,
    onInviteTextChange: (String) -> Unit,
    onSendInvite: () -> Unit,
    onInviteFriend: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onBack: () -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onSpotSelected: (StudySpotSummary) -> Unit
) {
    var showInviteSheet by remember { mutableStateOf(false) }
    var showLeaveSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        GroupModeHeader(
            groupSession = groupSession,
            onBack = onBack,
            onInvite = { showInviteSheet = true },
            onLeave = { showLeaveSheet = true },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 10.dp,
                end = 20.dp,
                bottom = 18.dp
            )
        ) {
            item {
                Text(
                    text = "Best group spots nearby",
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(14.dp))
                if (spots.isEmpty()) {
                    Text(
                        text = "No group-friendly places are currently available.",
                        color = BodyText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            itemsIndexed(
                items = spots,
                key = { _, spot -> spot.id }
            ) { _, spot ->
                SpotCard(
                    spot = spot,
                    accent = GroupGreen,
                    mode = SpotCardMode.Group,
                    onClick = { onSpotSelected(spot) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        BottomNavigationShell(
            accent = GroupGreen,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }

    if (showInviteSheet) {
        val currentMemberIds = groupSession.members.map { it.id }.toSet()
        GroupInviteSheet(
            friends = invitableFriends.filter { it.id !in currentMemberIds },
            invitedFriendIds = invitedGroupFriendIds,
            emailValue = inviteText,
            onEmailValueChange = onInviteTextChange,
            isSending = isActionInProgress,
            onSendEmail = {
                onSendInvite()
                showInviteSheet = false
            },
            onInviteFriend = onInviteFriend,
            onDismiss = { showInviteSheet = false }
        )
    }
    if (showLeaveSheet) {
        LeaveGroupSheet(
            isOwner = groupSession.isOwner,
            isLeaving = isActionInProgress,
            onConfirm = {
                onLeaveGroup()
                showLeaveSheet = false
            },
            onDismiss = { showLeaveSheet = false }
        )
    }
}

@Composable
private fun GroupModeHeader(
    groupSession: GroupStudySession,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onLeave: () -> Unit,
) {
    val isSolo = groupSession.members.size <= 1
    val visibilityLabel = if (groupSession.visibility == GroupVisibility.Public) {
        "Public group"
    } else {
        "Private group"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupHeaderTopRow(onBack, Modifier.weight(1f))
            Text(
                text = if (groupSession.isOwner) "End group" else "Leave",
                color = ModerateFitText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onLeave)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = groupSession.title,
            color = Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                append(visibilityLabel)
                append(" • ")
                append(if (isSolo) "1 member" else "${groupSession.members.size} members")
                groupSession.proximityLabel.takeIf { it.isNotBlank() }?.let {
                    append(" • ")
                    append(it)
                }
            },
            color = BodyText,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSolo) {
                GroupAvatarStrip(groupSession.members)
            }
            Spacer(Modifier.weight(1f))
            if (groupSession.isOwner) {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .border(1.5.dp, SoloBlue, RoundedCornerShape(13.dp))
                        .clickable(onClick = onInvite)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = SoloBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Invite member",
                        color = SoloBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeaderTopRow(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, DividerLine, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to map",
                tint = Ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .border(1.5.dp, GroupGreen, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = null,
                tint = GroupGreen,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Group mode",
                color = GroupGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GroupAvatarStrip(members: List<GroupMember>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        members.take(4).forEachIndexed { index, member ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(avatarColorFor(member.id), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.initials,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        val remaining = members.size - 4
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(GroupMoreAvatar, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remaining",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInviteSheet(
    friends: List<FriendProfile>,
    invitedFriendIds: Set<String> = emptySet(),
    emailValue: String,
    onEmailValueChange: (String) -> Unit,
    isSending: Boolean,
    onSendEmail: () -> Unit,
    onInviteFriend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val trimmedEmail = emailValue.trim()
    val emailLooksValid = trimmedEmail.contains("@") && trimmedEmail.substringAfterLast("@").contains(".")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Invite member", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Add friends or invite someone by email.", color = BodyText, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier.size(44.dp).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close invite", tint = Ink)
                }
            }

            if (friends.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "YOUR FRIENDS",
                    color = SectionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                friends.forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = friend.fullName,
                                color = Ink,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (friend.displayDetail.isNotBlank()) {
                                Text(
                                    text = friend.displayDetail,
                                    color = BodyText,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        val alreadyInvited = friend.id in invitedFriendIds
                        Button(
                            onClick = { if (!alreadyInvited) onInviteFriend(friend.id) },
                            enabled = !isSending && !alreadyInvited,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (alreadyInvited) SwitcherTrack else GroupGreen,
                                disabledContainerColor = if (alreadyInvited) SwitcherTrack else SwitcherTrack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (alreadyInvited) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp), tint = GroupGreen)
                                Spacer(Modifier.width(4.dp))
                                Text("Added", fontWeight = FontWeight.Bold, color = GroupGreen)
                            } else {
                                Text("Add", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerLine))
                    Text(
                        text = "  OR  ",
                        color = HeaderMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(DividerLine))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Invite by email", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(
                        1.dp,
                        if (emailValue.isNotBlank() && !emailLooksValid) ModerateFitText else DividerLine,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.MailOutline, contentDescription = null, tint = BodyText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = emailValue,
                    onValueChange = onEmailValueChange,
                    enabled = !isSending,
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (emailValue.isBlank()) Text("name@uwaterloo.ca", color = HeaderMuted, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            if (emailValue.isNotBlank() && !emailLooksValid) {
                Spacer(Modifier.height(6.dp))
                Text("Enter a complete email address.", color = ModerateFitText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = onSendEmail,
                    enabled = emailLooksValid && !isSending,
                    modifier = Modifier.weight(1.4f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoloBlue,
                        disabledContainerColor = SwitcherTrack,
                        disabledContentColor = HeaderMuted
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send invitation", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveGroupSheet(
    isOwner: Boolean,
    isLeaving: Boolean,
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
                text = if (isOwner) "End this group?" else "Leave this group?",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isOwner) {
                    "The group will close for every member. You can create a new one whenever you want."
                } else {
                    "You’ll be removed from the group, but the other members can keep studying together."
                },
                color = BodyText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLeaving,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !isLeaving,
                    modifier = Modifier.weight(1.2f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ModerateFitText)
                ) {
                    if (isLeaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = if (isOwner) "End group" else "Leave group",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
