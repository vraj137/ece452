package com.appetizers.spotra.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appetizers.spotra.BuildConfig
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.SpotFeature
import com.appetizers.spotra.domain.model.SpotFeatureType
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.HomeRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(homeRepository: HomeRepository) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(homeRepository)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent by animateColorAsState(
        targetValue = if (state.selectedMode == StudyMode.Solo) SoloBlue else GroupGreen,
        label = "mode-accent"
    )

    if (state.isLoading || state.soloSpot == null || state.groupSession == null) {
        HomeLoadingScreen()
        return
    }
    val soloSpot = state.soloSpot ?: return
    val groupSession = state.groupSession ?: return

    state.activeCheckIn?.let { session ->
        LiveCheckInScreen(
            session = session,
            accent = session.mode.accentColor(),
            requestedBuddyIds = state.requestedBuddyIds,
            onBuddyRequest = viewModel::sendBuddyRequest,
            onBack = viewModel::closeCheckIn,
            onCheckout = viewModel::checkOut,
            selectedSection = state.selectedSection,
            onSectionSelected = { section ->
                viewModel.closeCheckIn()
                viewModel.selectSection(section)
            }
        )
        return
    }

    if (state.selectedSection == HomeSection.Map && state.selectedMode == StudyMode.Group) {
        BackHandler { viewModel.returnToSoloMap() }
        GroupModeScreen(
            groupSession = groupSession,
            spots = state.groupSpots,
            inviteText = state.inviteText,
            onInviteTextChange = viewModel::updateInviteText,
            onSendInvite = viewModel::sendGroupInvite,
            onBack = viewModel::returnToSoloMap,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection,
            onSpotSelected = { spot ->
                viewModel.startCheckIn(spot, StudyMode.Group)
            }
        )
        return
    }

    if (state.selectedSection == HomeSection.Explore) {
        val displayedSpot = state.mapSpots.firstOrNull { it.id == state.selectedSpotId } ?: soloSpot
        LiveSensorEnvironmentScreen(
            spot = displayedSpot,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection,
            onBack = { viewModel.selectSection(HomeSection.Map) },
            onCheckIn = { viewModel.startCheckIn(displayedSpot, state.selectedMode) }
        )
        return
    }

    if (state.selectedSection == HomeSection.Social) {
        SocialScreen(
            selectedTab = state.selectedSocialTab,
            onTabSelected = viewModel::selectSocialTab,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection,
            onJoin = {
                viewModel.startCheckIn(soloSpot, StudyMode.Solo)
            },
            onAddBuddy = viewModel::sendBuddyRequest,
            requestedBuddyIds = state.requestedBuddyIds
        )
        return
    }

    if (state.selectedSection == HomeSection.Profile) {
        ProfileScreen(
            userFirstName = state.userFirstName,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        HomeHeader(
            userFirstName = state.userFirstName,
            selectedMode = state.selectedMode,
            accent = accent,
            onModeSelected = viewModel::selectMode
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            CampusMap(
                spots = state.mapSpots,
                selectedSpotId = state.selectedSpotId,
                onSpotSelected = viewModel::selectMapSpot,
                accent = accent,
                mode = state.selectedMode,
                modifier = Modifier.fillMaxSize()
            )
            val displayedSpot = state.mapSpots.firstOrNull { it.id == state.selectedSpotId }
                ?: soloSpot
            StudySpotCard(
                spot = displayedSpot,
                accent = accent,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 30.dp, end = 30.dp, bottom = 16.dp),
                onClick = {
                    viewModel.startCheckIn(displayedSpot, StudyMode.Solo)
                }
            )
        }
        BottomNavigationShell(
            accent = accent,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection
        )
    }
}

@Composable
private fun HomeLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Loading study spots...",
            color = HeaderMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GroupModeScreen(
    groupSession: GroupStudySession,
    spots: List<StudySpotSummary>,
    inviteText: String,
    onInviteTextChange: (String) -> Unit,
    onSendInvite: () -> Unit,
    onBack: () -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onSpotSelected: (StudySpotSummary) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        GroupModeHeader(groupSession, onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 38.dp,
                top = 18.dp,
                end = 32.dp,
                bottom = 18.dp
            )
        ) {
            item {
                Text(
                    text = "BEST GROUP SPOTS NEARBY",
                    color = SectionLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Spacer(Modifier.height(14.dp))
            }
            itemsIndexed(
                items = spots,
                key = { _, spot -> spot.id }
            ) { _, spot ->
                GroupSpotCard(
                    spot = spot,
                    onClick = { onSpotSelected(spot) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
        GroupInviteBar(
            value = inviteText,
            onValueChange = onInviteTextChange,
            onSend = onSendInvite
        )
        BottomNavigationShell(
            accent = GroupGreen,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }
}

@Composable
private fun GroupModeHeader(groupSession: GroupStudySession, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroupHeaderGreen)
            .padding(start = 40.dp, top = 48.dp, end = 32.dp, bottom = 28.dp)
    ) {
        GroupHeaderTopRow(onBack)
        Spacer(Modifier.height(22.dp))
        Text(
            text = groupSession.title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${groupSession.subtitle} - ${groupSession.members.size} members",
            color = GroupHeaderSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupAvatarStrip(groupSession.members)
            Spacer(Modifier.width(18.dp))
            Text(
                text = groupSession.proximityLabel,
                color = GroupHeaderSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GroupHeaderTopRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(GroupBackButton, RoundedCornerShape(13.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to map",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .background(GroupHeaderChip, RoundedCornerShape(22.dp))
                .padding(horizontal = 17.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Group mode",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
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
                    .background(avatarColorFor(member.id, index), CircleShape),
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

@Composable
private fun GroupSpotCard(
    spot: StudySpotSummary,
    onClick: () -> Unit
) {
    val borderColor = if (spot.bestFit) GroupGreen else GroupCardBorder
    val backgroundColor = if (spot.bestFit) GroupBestFitBackground else Color.White
    val icon = groupSpotIconFor(spot)
    val iconBackground = if (spot.bestFit) GroupSpotIconGreen else GroupSpotIconYellow
    val iconTint = if (spot.bestFit) GroupGreen else Ink
    val fitBackground = if (spot.badge == "Moderate") ModerateFitBackground else QuietPill
    val fitTextColor = if (spot.badge == "Moderate") ModerateFitText else QuietText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBackground, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = spot.name,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = spot.badge,
                modifier = Modifier
                    .background(fitBackground, RoundedCornerShape(16.dp))
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                color = fitTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(14.dp))
        GroupFeatureRows(spot.features)
    }
}

@Composable
private fun GroupFeatureRows(features: List<SpotFeature>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        features.chunked(2).forEach { rowFeatures ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowFeatures.forEach { feature ->
                    GroupFeatureChip(feature)
                }
            }
        }
    }
}

@Composable
private fun GroupFeatureChip(feature: SpotFeature) {
    Row(
        modifier = Modifier
            .background(GroupFeatureChipBackground, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconForFeature(feature.type),
            contentDescription = null,
            tint = BodyText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = feature.label,
            color = BodyText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun GroupInviteBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = value.trim().isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, DividerLine)
            .padding(start = 38.dp, top = 14.dp, end = 32.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .background(InviteInputBackground, RoundedCornerShape(16.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = false,
                textStyle = TextStyle(
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isBlank()) {
                Text(
                    text = "Invite a friend to this session...",
                    color = HeaderMuted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .height(72.dp)
                .background(
                    if (canSend) GroupGreen else DisabledSend,
                    RoundedCornerShape(16.dp)
                )
                .clickable(enabled = canSend, onClick = onSend)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send invite",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Send",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun LiveSensorEnvironmentScreen(
    spot: StudySpotSummary,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onBack: () -> Unit,
    onCheckIn: () -> Unit
) {
    val readings = remember(spot.id) { sensorReadingsFor(spot.id) }
    val score = remember(readings) { readings.sumOf { it.scoreWeight } / readings.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CheckInHeader)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.padding(start = 30.dp, top = 34.dp, end = 30.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(HeaderButton, RoundedCornerShape(14.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = spot.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Live sensor readings - Updated 12s ago",
                color = HeaderSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .background(SensorScoreBackground, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Color(0xFF9BB2FF),
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$score / 100 study score",
                    color = Color(0xFFDCE4FF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        SensorPrivacyNote()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp,
                top = 26.dp,
                end = 24.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(readings.chunked(2)) { _, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { reading ->
                        SensorReadingCard(
                            reading = reading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(SoloBlue, RoundedCornerShape(18.dp))
                        .clickable(onClick = onCheckIn),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Check in here",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        BottomNavigationShell(
            accent = SoloBlue,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }
}

@Composable
private fun SensorPrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F4FF))
            .padding(horizontal = 30.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SoloBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "Readings from your phone's mic, light sensor & network",
            color = SoloBlue,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SensorReadingCard(
    reading: SensorReading,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(178.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(reading.tint.copy(alpha = .13f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = reading.icon,
                    contentDescription = null,
                    tint = reading.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = reading.label,
                color = SectionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = reading.value,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = reading.description,
            color = HeaderMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(DividerLine, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(reading.progress)
                    .height(5.dp)
                    .background(reading.tint, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun SocialScreen(
    selectedTab: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
    onJoin: () -> Unit,
    onAddBuddy: (String) -> Unit,
    requestedBuddyIds: Set<String>
) {
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
            Text(
                text = "Social",
                color = Ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(22.dp))
            SocialTabs(selectedTab, onTabSelected)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 30.dp,
                top = 22.dp,
                end = 30.dp,
                bottom = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                SocialTab.Friends -> {
                    item { SectionHeader("STUDYING NOW") }
                    itemsIndexed(friendStudyRows()) { _, friend ->
                        SocialPersonRow(
                            person = friend,
                            actionLabel = if (friend.active) "Join" else "Offline",
                            actionEnabled = friend.active,
                            onAction = onJoin
                        )
                    }
                    item { StudyStreakCard() }
                }
                SocialTab.Buddies -> {
                    item { SectionHeader("CONFIRMED STUDY BUDDIES") }
                    itemsIndexed(confirmedBuddies()) { _, buddy ->
                        SocialPersonRow(
                            person = buddy,
                            actionLabel = "Message",
                            actionEnabled = true,
                            onAction = {}
                        )
                    }
                    item { StudyStreakCard() }
                }
                SocialTab.Discover -> {
                    item { SectionHeader("SUGGESTED FROM YOUR COURSES") }
                    itemsIndexed(discoverBuddies()) { _, buddy ->
                        SocialPersonRow(
                            person = buddy,
                            actionLabel = if (buddy.id in requestedBuddyIds) "Sent" else "+ Add",
                            actionEnabled = buddy.id !in requestedBuddyIds,
                            onAction = { onAddBuddy(buddy.id) }
                        )
                    }
                    item { StudyStreakCard() }
                }
            }
        }
        BottomNavigationShell(
            accent = SoloBlue,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = tab.name,
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
private fun SectionHeader(label: String) {
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
    onAction: () -> Unit
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
        Text(
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
}

@Composable
private fun StudyStreakCard() {
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
                text = "4 day study streak",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Check in tomorrow to keep it alive.",
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    userFirstName: String,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text(
                    text = "$userFirstName's study profile",
                    color = Ink,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            item {
                StudyStreakCard()
            }
            item {
                ProfileMetricCard("Preferred mode", "Solo mornings, group evenings", Icons.Rounded.Person)
            }
            item {
                ProfileMetricCard("Top course match", "CS 341 - 8 active buddies", Icons.Rounded.School)
            }
            item {
                ProfileMetricCard("Privacy", "Buddy requests are opt-in both ways", Icons.Rounded.CheckCircle)
            }
        }
        BottomNavigationShell(
            accent = SoloBlue,
            selectedSection = selectedSection,
            onSectionSelected = onSectionSelected
        )
    }
}

@Composable
private fun ProfileMetricCard(title: String, detail: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(SelfPillBackground, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SoloBlue, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = HeaderMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LiveCheckInScreen(
    session: CheckInSession,
    accent: Color,
    requestedBuddyIds: Set<String>,
    onBuddyRequest: (String) -> Unit,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    var elapsedSeconds by remember(session.spot.name) { mutableStateOf(0) }
    var selectedBuddy by remember(session.id) { mutableStateOf<CheckedInStudent?>(null) }

    LaunchedEffect(session.spot.name) {
        while (true) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            LiveCheckInHeader(
                spotName = session.spot.name,
                peopleHere = session.attendees.size,
                onBack = onBack
            )
            CheckInAttendeeList(
                students = session.attendees,
                requestedBuddyIds = requestedBuddyIds,
                onBuddyClick = { student -> selectedBuddy = student },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            CheckInSessionPanel(
                elapsedSeconds = elapsedSeconds,
                onCheckout = onCheckout
            )
            BottomNavigationShell(
                accent = accent,
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected
            )
        }

        selectedBuddy?.let { buddy ->
            BuddyRequestSheet(
                student = buddy,
                requested = buddy.id in requestedBuddyIds,
                onDismiss = { selectedBuddy = null },
                onAccept = {
                    onBuddyRequest(buddy.id)
                    selectedBuddy = null
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CheckInAttendeeList(
    students: List<CheckedInStudent>,
    requestedBuddyIds: Set<String>,
    onBuddyClick: (CheckedInStudent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 36.dp,
                top = 20.dp,
                end = 24.dp,
                bottom = 104.dp
            )
        ) {
            item {
                Text(
                    text = "WHO'S HERE",
                    color = SectionLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))
            }
            if (students.isEmpty()) {
                item {
                    Text(
                        text = "No one else is checked in here yet.",
                        color = HeaderMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = students,
                    key = { _, student -> student.id }
                ) { index, student ->
                    CheckedInStudentRow(
                        student = student,
                        requested = student.id in requestedBuddyIds,
                        onBuddyClick = { onBuddyClick(student) }
                    )
                    if (index < students.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DividerLine)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White)
                    )
                )
        )
    }
}

@Composable
private fun LiveCheckInHeader(
    spotName: String,
    peopleHere: Int,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CheckInHeader)
            .padding(start = 40.dp, top = 54.dp, end = 32.dp, bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(HeaderButton, RoundedCornerShape(14.dp))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = spotName,
            color = Color.White,
            fontSize = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "You checked in - $peopleHere people studying now",
            color = HeaderSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .background(CheckedInPill, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(CheckedInDot, CircleShape)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "Checked in",
                color = CheckedInText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CheckedInStudentRow(
    student: CheckedInStudent,
    requested: Boolean,
    onBuddyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (student.isSelf) SelfRowBackground else Color.White)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudentAvatar(student)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = student.name,
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (student.isFriend) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "friend",
                        color = SoloBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = student.detail,
                color = HeaderMuted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        when {
            student.isSelf -> RelationPill("You", SoloBlue, SelfPillBackground)
            student.isFriend -> Text(
                text = "Connected",
                color = HeaderMuted,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            else -> BuddyRequestButton(
                requested = requested,
                onClick = onBuddyClick
            )
        }
    }
}

@Composable
private fun StudentAvatar(student: CheckedInStudent) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(avatarColorFor(student.id), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = student.initials,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun RelationPill(
    text: String,
    contentColor: Color,
    backgroundColor: Color
) {
    Text(
        text = text,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 13.dp, vertical = 7.dp),
        color = contentColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

@Composable
private fun BuddyRequestButton(requested: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (requested) RequestedPill else BuddyPill, RoundedCornerShape(18.dp))
            .clickable(enabled = !requested, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (requested) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
            contentDescription = if (requested) "Buddy request sent" else "Send buddy request",
            tint = SoloBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (requested) "Sent" else "Buddy",
            color = SoloBlue,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun BuddyRequestSheet(
    student: CheckedInStudent,
    requested: Boolean,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .background(Color.White, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(start = 28.dp, top = 18.dp, end = 28.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(70.dp)
                    .height(5.dp)
                    .background(DividerLine, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(avatarColorFor(student.id), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.initials,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = student.name,
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = buddyMeta(student),
                color = HeaderMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buddyTags(student).take(3).forEach { tag ->
                    RelationPill(tag, BodyText, SwitcherTrack)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buddyTags(student).drop(3).forEach { tag ->
                    RelationPill(tag, BodyText, SwitcherTrack)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "\"Hey! Saw you're in CS 341 too. Want to be study buddies for finals week?\"",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InviteInputBackground, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                color = BodyText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Decline",
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .background(SwitcherTrack, RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss)
                        .padding(top = 16.dp),
                    color = HeaderMuted,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier
                        .weight(1.45f)
                        .height(54.dp)
                        .background(if (requested) RequestedPill else SoloBlue, RoundedCornerShape(16.dp))
                        .clickable(enabled = !requested, onClick = onAccept),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (requested) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = if (requested) QuietText else Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (requested) "Request sent" else "Accept buddy",
                        color = if (requested) QuietText else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInSessionPanel(
    elapsedSeconds: Int,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, DividerLine)
            .padding(start = 36.dp, top = 14.dp, end = 24.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SessionClockIcon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Session time",
                color = BodyText,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = elapsedSeconds.asSessionTime(),
                color = Ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(SwitcherTrack, RoundedCornerShape(15.dp))
                .clickable(onClick = onCheckout),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = BodyText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Check out & review",
                color = BodyText,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun SessionClockIcon() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(SelfPillBackground, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val stroke = 2.4.dp.toPx()
            drawCircle(
                color = SoloBlue,
                radius = size.minDimension / 2 - stroke,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            drawLine(
                color = SoloBlue,
                start = center,
                end = Offset(center.x, center.y - size.height * .26f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = SoloBlue,
                start = center,
                end = Offset(center.x + size.width * .22f, center.y + size.height * .12f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun HomeHeader(
    userFirstName: String,
    selectedMode: StudyMode,
    accent: Color,
    onModeSelected: (StudyMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 38.dp, top = 30.dp, end = 28.dp, bottom = 26.dp)
    ) {
        SpotraLandingWordmark(accent)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Good afternoon, $userFirstName",
            color = HeaderMuted,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(24.dp))
        ModeSwitcher(
            selectedMode = selectedMode,
            accent = accent,
            onModeSelected = onModeSelected
        )
    }
}

@Composable
private fun SpotraLandingWordmark(accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "sp",
            color = Ink,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "o",
            color = accent,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "tra",
            color = Ink,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ModeSwitcher(
    selectedMode: StudyMode,
    accent: Color,
    onModeSelected: (StudyMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(SwitcherTrack, RoundedCornerShape(36.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeSegment(
            label = "Solo",
            icon = Icons.Rounded.Person,
            selected = selectedMode == StudyMode.Solo,
            selectedColor = accent,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(StudyMode.Solo) }
        )
        ModeSegment(
            label = "Group",
            icon = Icons.Rounded.Group,
            selected = selectedMode == StudyMode.Group,
            selectedColor = accent,
            modifier = Modifier.weight(1f),
            onClick = { onModeSelected(StudyMode.Group) }
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = if (selected) selectedColor else Color.Transparent,
                shape = RoundedCornerShape(30.dp)
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val contentColor = if (selected) Color.White else MutedText
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CampusMap(
    spots: List<StudySpotSummary>,
    selectedSpotId: String?,
    onSpotSelected: (String) -> Unit,
    accent: Color,
    mode: StudyMode,
    modifier: Modifier = Modifier
) {
    if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank()) {
        CampusMapPlaceholder(mode = mode, accent = accent, modifier = modifier)
        return
    }

    val located = spots.filter { it.latitude != null && it.longitude != null }
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-80.5430, 43.4720))
            zoom(14.6)
        }
    }

    Box(modifier = modifier.background(MapLoadingBackground)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.LIGHT) },
            scaleBar = {},
            logo = {},
            attribution = {}
        ) {
            located.forEach { spot ->
                key(spot.id) {
                    val options = remember(spot.id) {
                        viewAnnotationOptions {
                            geometry(Point.fromLngLat(spot.longitude!!, spot.latitude!!))
                            annotationAnchor {
                                anchor(ViewAnnotationAnchor.BOTTOM)
                            }
                            allowOverlap(true)
                        }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    ViewAnnotation(options = options) {
                        MapPin(
                            label = spot.name,
                            color = accent,
                            selected = spot.id == selectedSpotId,
                            modifier = Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSpotSelected(spot.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampusMapPlaceholder(
    mode: StudyMode,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val pins = if (mode == StudyMode.Solo) soloPins(accent) else groupPins(accent)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MapBackground)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(MapBackground)

            val blockColor = MapBlock.copy(alpha = .88f)
            val blockRadius = 9.dp.toPx()
            val blockWidth = size.width * .24f
            val blockHeight = size.height * .20f
            val blockPositions = listOf(
                Offset(size.width * .05f, size.height * .10f),
                Offset(size.width * .31f, size.height * .13f),
                Offset(size.width * .63f, size.height * .10f),
                Offset(size.width * .05f, size.height * .47f),
                Offset(size.width * .31f, size.height * .49f),
                Offset(size.width * .63f, size.height * .49f)
            )
            blockPositions.forEach { topLeft ->
                drawRoundRect(
                    color = blockColor,
                    topLeft = topLeft,
                    size = Size(blockWidth, blockHeight),
                    cornerRadius = CornerRadius(blockRadius, blockRadius)
                )
            }

            val streetWidth = 13.dp.toPx()
            listOf(.26f, .58f, .91f).forEach { x ->
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * x, 0f),
                    end = Offset(size.width * x, size.height),
                    strokeWidth = streetWidth,
                    cap = StrokeCap.Round
                )
            }
            listOf(.31f, .59f, .87f).forEach { y ->
                drawLine(
                    color = Color.White,
                    start = Offset(0f, size.height * y),
                    end = Offset(size.width, size.height * y),
                    strokeWidth = streetWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        pins.forEach { pin ->
            MapPin(
                label = pin.label,
                color = pin.color,
                selected = pin.selected,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * pin.x, y = maxHeight * pin.y)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = maxWidth * .72f, y = maxHeight * .65f)
                .size(22.dp)
                .background(Color.White, CircleShape)
                .padding(4.dp)
                .background(SoloBlue, CircleShape)
        )
    }
}

@Composable
private fun MapPin(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    // Every spot always shows its label pill + dot. Focus is expressed purely through a
    // draw-time scale and a fading white ring (no re-measure), so the Mapbox ViewAnnotation
    // never repositions — the pin grows in place, anchored at its dot, instead of shaking.
    val pillScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pill-scale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pill-border"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1.25f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dot-scale"
    )

    val pillShape = RoundedCornerShape(18.dp)
    // Reserve transparent room on top/sides — never the bottom — so the dot stays pinned to
    // the map coordinate while the pill (and its shadow) render above without being clipped.
    Box(
        modifier = modifier.padding(top = 14.dp, start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pillScale
                        scaleY = pillScale
                        // Grow up out of the dot.
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .shadow(6.dp, pillShape, clip = false)
                    .background(color, pillShape)
                    .border(2.dp, Color.White.copy(alpha = borderAlpha), pillShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Base marker — a dot that always marks the spot, enlarging slightly when selected.
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                    }
                    .size(15.dp)
                    .shadow(3.dp, CircleShape, clip = false)
                    .background(Color.White, CircleShape)
                    .padding(3.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
private fun StudySpotCard(
    spot: StudySpotSummary,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
    val distanceLabel = spot.distanceMeters?.let { "${it}m -" }
    val contextLabel = spot.studyContextLabel.orEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(118.dp)
            .background(CardBackground, RoundedCornerShape(20.dp))
            .border(3.dp, accent, RoundedCornerShape(20.dp))
            .then(clickModifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color(0xFFE7E8FF), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.School,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(29.dp)
            )
        }
        Spacer(Modifier.width(18.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = spot.name,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                distanceLabel?.let {
                    Text(
                        text = it,
                        color = HeaderMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                spot.rating?.let { rating ->
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " $rating",
                        color = HeaderMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (contextLabel.isNotBlank()) {
                    Text(
                        text = " - $contextLabel",
                        color = HeaderMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = spot.badge,
            modifier = Modifier
                .background(QuietPill, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            color = QuietText,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun BottomNavigationShell(
    accent: Color,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Map",
            icon = Icons.Rounded.Map,
            selected = selectedSection == HomeSection.Map,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onSectionSelected(HomeSection.Map) }
        )
        BottomNavItem(
            label = "Explore",
            icon = Icons.Rounded.Explore,
            selected = selectedSection == HomeSection.Explore,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onSectionSelected(HomeSection.Explore) }
        )
        BottomNavItem(
            label = "Social",
            icon = Icons.Rounded.Group,
            selected = selectedSection == HomeSection.Social,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onSectionSelected(HomeSection.Social) }
        )
        BottomNavItem(
            label = "Profile",
            icon = Icons.Rounded.AccountCircle,
            selected = selectedSection == HomeSection.Profile,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { onSectionSelected(HomeSection.Profile) }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) accent else NavMuted
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private data class StudyMapPin(
    val label: String,
    val x: Float,
    val y: Float,
    val color: Color,
    val selected: Boolean = false
)

private data class SensorReading(
    val label: String,
    val value: String,
    val description: String,
    val progress: Float,
    val scoreWeight: Int,
    val icon: ImageVector,
    val tint: Color
)

private data class SocialPerson(
    val id: String,
    val initials: String,
    val name: String,
    val detail: String,
    val active: Boolean = true
)

private fun StudyMode.accentColor(): Color = when (this) {
    StudyMode.Solo -> SoloBlue
    StudyMode.Group -> GroupGreen
}

private fun sensorReadingsFor(spotId: String): List<SensorReading> {
    val quietBias = if (spotId.contains("library") || spotId.contains("e7")) 0 else 8
    return listOf(
        SensorReading(
            label = "NOISE",
            value = "${32 + quietBias} dB",
            description = if (quietBias == 0) "Library-quiet" else "Moderate chatter",
            progress = if (quietBias == 0) .42f else .62f,
            scoreWeight = if (quietBias == 0) 96 else 78,
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            tint = GroupGreen
        ),
        SensorReading(
            label = "LIGHTING",
            value = "480 lx",
            description = "Ideal for reading",
            progress = .86f,
            scoreWeight = 92,
            icon = Icons.Rounded.LightMode,
            tint = StarGold
        ),
        SensorReading(
            label = "WI-FI",
            value = "94 Mbps",
            description = "Excellent signal",
            progress = .94f,
            scoreWeight = 94,
            icon = Icons.Rounded.Wifi,
            tint = SoloBlue
        ),
        SensorReading(
            label = "OCCUPANCY",
            value = "26%",
            description = "Plenty of seats",
            progress = .34f,
            scoreWeight = 82,
            icon = Icons.Rounded.Groups,
            tint = GroupGreen
        )
    )
}

private fun friendStudyRows() = listOf(
    SocialPerson("raghav", "RV", "Raghav Verma", "E7 Study Hall - 1h 2min"),
    SocialPerson("vishvam", "VP", "Vishvam Patel", "DC Library 3F - 28min"),
    SocialPerson("edmond", "EY", "Edmond Yang", "Last seen 3h ago", active = false)
)

private fun confirmedBuddies() = listOf(
    SocialPerson("akshat", "AJ", "Akshat Jawne", "ECE 222 - quiet morning sessions"),
    SocialPerson("eric", "EZ", "Eric Z.", "MATH 237 - problem set partner"),
    SocialPerson("raghav", "RV", "Raghav Verma", "CS 341 - finals prep")
)

private fun discoverBuddies() = listOf(
    SocialPerson("kira", "KL", "Kira L.", "CS 341 - graph algorithms"),
    SocialPerson("max", "MP", "Max P.", "ECE 222 - circuits review"),
    SocialPerson("sara", "SN", "Sara N.", "CS 341 - quiet study")
)

private fun buddyMeta(student: CheckedInStudent): String = when (student.id) {
    "akshat" -> "2B Software Eng - 12 study sessions"
    "eric" -> "2A Math - 8 study sessions"
    "raghav" -> "3A CS - 18 study sessions"
    else -> "${student.detail.substringBefore(" - ")} - study nearby"
}

private fun buddyTags(student: CheckedInStudent): List<String> = when (student.id) {
    "akshat" -> listOf("ECE 222", "CS 341", "Quiet studier", "Morning sessions")
    "eric" -> listOf("MATH 237", "CS 341", "Problem sets", "Whiteboard")
    "raghav" -> listOf("CS 341", "ECE 298", "Friend", "Evening sessions")
    else -> listOf("CS 341", "Focused", "Solo-friendly", "Open to buddy")
}

private fun soloPins(accent: Color) = listOf(
    StudyMapPin("E7 Hall", .64f, .20f, accent, selected = true),
    StudyMapPin("DC Lib", .33f, .35f, LibraryGreen),
    StudyMapPin("SLC 2F", .07f, .65f, SLCOrange),
    StudyMapPin("DP Atrium", .66f, .64f, DPAtriumRed)
)

private fun groupPins(accent: Color) = listOf(
    StudyMapPin("DC Team", .29f, .31f, accent, selected = true),
    StudyMapPin("E7 Hall", .63f, .20f, SoloBlue),
    StudyMapPin("SLC 2F", .07f, .65f, SLCOrange),
    StudyMapPin("EV3 Hub", .63f, .64f, LibraryGreen)
)

private fun avatarColorFor(id: String, index: Int = 0): Color = when (id) {
    "you" -> SoloBlue
    "akshat" -> PurpleAvatar
    "eric" -> LibraryGreen
    "raghav" -> SLCOrange
    "pavan" -> Color(0xFF0EA5E9)
    "edmond" -> Color(0xFF14B8A6)
    "maya" -> Color(0xFF8B5CF6)
    "leah" -> DPAtriumRed
    "kai" -> Color(0xFF64748B)
    "priya" -> Color(0xFFEF4444)
    "ben" -> Color(0xFFF97316)
    "lina" -> Color(0xFF22C55E)
    else -> listOf(
        Color(0xFF14B8A6),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFF22C55E)
    )[index.coerceAtLeast(0) % 5]
}

private fun iconForFeature(type: SpotFeatureType): ImageVector = when (type) {
    SpotFeatureType.Seating -> Icons.Rounded.Group
    SpotFeatureType.Whiteboard -> Icons.Rounded.Check
    SpotFeatureType.Wifi -> Icons.Rounded.Explore
    SpotFeatureType.Accessible -> Icons.Rounded.Person
    SpotFeatureType.Outlets -> Icons.Rounded.Check
    SpotFeatureType.Noise -> Icons.Rounded.Person
    SpotFeatureType.Projector -> Icons.Rounded.Map
    SpotFeatureType.NearbyCafe -> Icons.Rounded.Star
}

private fun groupSpotIconFor(spot: StudySpotSummary): ImageVector = when {
    spot.bestFit -> Icons.Rounded.School
    spot.features.any { it.type == SpotFeatureType.Projector } -> Icons.Rounded.Group
    else -> Icons.Rounded.Map
}

private fun Int.asSessionTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private val HomeBackground = Color(0xFFF8F7F3)
private val Ink = Color(0xFF171A3C)
private val HeaderMuted = Color(0xFFA8A6AA)
private val MutedText = Color(0xFF96949A)
private val SwitcherTrack = Color(0xFFEDE9E0)
private val SoloBlue = Color(0xFF4355E8)
private val SensorScoreBackground = Color(0xFF293B8E)
private val GroupGreen = Color(0xFF21A46F)
private val GroupHeaderGreen = Color(0xFF115C3B)
private val GroupHeaderChip = Color(0xFF3A7B62)
private val GroupBackButton = Color(0xFF0D4A31)
private val GroupHeaderSecondary = Color(0xFFB7D0C3)
private val GroupMoreAvatar = Color(0xFF6A8176)
private val GroupBestFitBackground = Color(0xFFEFFAF4)
private val GroupCardBorder = Color(0xFFEDE9E0)
private val GroupFeatureChipBackground = Color(0xFFEDE9E0)
private val GroupSpotIconGreen = Color(0xFFDDF6EA)
private val GroupSpotIconYellow = Color(0xFFFFF1D2)
private val ModerateFitBackground = Color(0xFFFFF1D2)
private val ModerateFitText = Color(0xFF8A5200)
private val InviteInputBackground = Color(0xFFF1F0ED)
private val DisabledSend = Color(0xFF9ACFB9)
private val MapBackground = Color(0xFFEAE6DB)
private val MapLoadingBackground = Color(0xFFE8E8E8)
private val MapBlock = Color(0xFFCFC9B6)
private val CardBackground = Color(0xFFF0F1FF)
private val QuietPill = Color(0xFFDDF6EA)
private val QuietText = Color(0xFF137B4B)
private val LibraryGreen = Color(0xFF249B6C)
private val SLCOrange = Color(0xFFD89209)
private val DPAtriumRed = Color(0xFFD83D3C)
private val NavMuted = Color(0xFFC2BEB5)
private val StarGold = Color(0xFFF8BC3B)
private val CheckInHeader = Color(0xFF1B1A31)
private val HeaderButton = Color(0xFF3A394F)
private val HeaderSecondary = Color(0xFF9D9AA9)
private val CheckedInPill = Color(0xFF244F50)
private val CheckedInDot = Color(0xFF5AE5A0)
private val CheckedInText = Color(0xFF72E6A7)
private val SectionLabel = Color(0xFFA5A5A7)
private val DividerLine = Color(0xFFECE9E3)
private val SelfRowBackground = Color(0xFFF3F3FD)
private val SelfPillBackground = Color(0xFFE8E9FF)
private val BuddyPill = Color(0xFFF0F0FF)
private val RequestedPill = Color(0xFFE7F8EF)
private val BodyText = Color(0xFF5F5C62)
private val PurpleAvatar = Color(0xFFA43CE2)
