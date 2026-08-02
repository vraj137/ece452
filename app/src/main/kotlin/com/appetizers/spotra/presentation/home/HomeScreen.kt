package com.appetizers.spotra.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appetizers.spotra.BuildConfig
import com.appetizers.spotra.data.location.LocationRepository
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.GroupMember
import com.appetizers.spotra.domain.model.GroupStudySession
import com.appetizers.spotra.domain.model.GroupVisibility
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import com.appetizers.spotra.presentation.toUserMessage
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import com.appetizers.spotra.domain.model.BadgeId
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeRepository: HomeRepository,
    profileRepository: ProfileRepository,
    authRepository: AuthRepository,
    spotSubmissionRepository: com.appetizers.spotra.domain.repository.SpotSubmissionRepository,
    reviewRepository: com.appetizers.spotra.domain.repository.ReviewRepository,
    friendRepository: FriendRepository,
    badgeRepository: BadgeRepository,
    streakRepository: StreakRepository,
    awardBadgesUseCase: AwardBadgesUseCase,
    locationRepository: LocationRepository,
    loginStreak: Int = 0,
    onSignOut: () -> Unit = {}
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            homeRepository,
            authRepository,
            streakRepository,
            reviewRepository,
            awardBadgesUseCase,
            locationRepository,
            friendRepository,
        )
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.updateUserLocation()
        }
    }
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = if (state.selectedMode == StudyMode.Solo) SoloBlue else GroupGreen

    var viewingSpotPath by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var reviewingSpotId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSubmitSpot by rememberSaveable { mutableStateOf(false) }
    var editingReview by remember { mutableStateOf<Review?>(null) }
    val viewingSpotId = viewingSpotPath.lastOrNull()

    val snackbarHostState = remember { SnackbarHostState() }
    val reviewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.newBadge) {
        state.newBadge?.let { badge ->
            snackbarHostState.showSnackbar("Badge earned: ${badge.label}!")
            viewModel.clearNewBadge()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    if (state.showReviewPrompt && state.pendingReviewSpotId != null) {
        PostCheckoutReviewSheet(
            spotName = state.pendingReviewSpotName ?: "this spot",
            sheetState = reviewSheetState,
            onSubmit = { rating, noiseLevel, lighting, wifiQuality, occupancyPercent, comment ->
                viewModel.submitPostCheckoutReview(rating, noiseLevel, lighting, wifiQuality, occupancyPercent, comment)
            },
            onDismiss = viewModel::dismissReviewPrompt,
        )
    }

    if (state.isLoading) {
        HomeLoadingScreen()
        return
    }
    if (state.soloSpot == null) {
        HomeUnavailableScreen(
            message = state.loadError ?: "No live study spots are available right now.",
            onRetry = viewModel::retryLoad
        )
        return
    }
    val soloSpot = state.soloSpot ?: return
    val groupSession = state.groupSession
    val activeCheckIn = state.activeCheckIn

    if (showSubmitSpot) {
        SubmitSpotScreen(
            authRepository = authRepository,
            spotSubmissionRepository = spotSubmissionRepository,
            onBack = { showSubmitSpot = false }
        )
        return
    }

    reviewingSpotId?.let { slug ->
        ReviewScreen(
            spotName = state.mapSpots.firstOrNull { it.id == slug }?.name ?: "this spot",
            spotSlug = slug,
            reviewRepository = reviewRepository,
            existingReview = editingReview,
            onBack = {
                reviewingSpotId = null
                editingReview = null
            }
        )
        return
    }

    viewingSpotId?.let { spotId ->
        val parentSpot = state.mapSpots.firstOrNull { it.id == spotId && it.childCount > 0 }
        if (parentSpot != null) {
            BuildingSpacesScreen(
                parentSpot = parentSpot,
                homeRepository = homeRepository,
                accent = accent,
                liveOccupancyBySpot = state.occupancyBySpot,
                onBack = { viewingSpotPath = previousSpotPath(viewingSpotPath) },
                onSpaceSelected = { child ->
                    viewingSpotPath = childSpotPath(viewingSpotPath, child.id)
                }
            )
            return
        }

        Box(Modifier.fillMaxSize()) {
            SpotDetailScreen(
                spotId = spotId,
                accent = accent,
                onBack = { viewingSpotPath = previousSpotPath(viewingSpotPath) },
                onCheckIn = { spot ->
                    viewModel.startCheckIn(spot, state.selectedMode) {
                        viewingSpotPath = emptyList()
                    }
                },
                checkInLabel = if (state.selectedMode == StudyMode.Group) "Join with group" else "Start Session",
                homeRepository = homeRepository,
                reviewRepository = reviewRepository,
                friendRepository = friendRepository,
                liveOccupancy = state.occupancyBySpot[spotId],
                onReview = {
                    editingReview = null
                    reviewingSpotId = spotId
                },
                onEditReview = { review ->
                    editingReview = review
                    reviewingSpotId = spotId
                },
                activeCheckInSpotId = activeCheckIn?.spot?.id,
                activeCheckInSpotName = activeCheckIn?.spot?.name?.takeIf { activeCheckIn.spot.id != spotId },
                onEndSession = {
                    viewModel.checkOut()
                    viewingSpotPath = emptyList()
                }
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }
        return
    }

    if (activeCheckIn != null && state.showLiveSession) {
        LiveCheckInScreen(
            session = activeCheckIn,
            sessionStartTimeMillis = state.sessionStartTimeMillis,
            accent = activeCheckIn.mode.accentColor(),
            requestedBuddyIds = state.requestedBuddyIds,
            onBuddyRequest = viewModel::sendBuddyRequest,
            onBack = viewModel::minimizeSession,
            onCheckout = { viewModel.checkOut() },
            selectedSection = state.selectedSection,
            onSectionSelected = { section ->
                viewModel.minimizeSession()
                viewModel.selectSection(section)
            }
        )
        return
    }

    if (state.selectedSection == HomeSection.Map && state.selectedMode == StudyMode.Group) {
        LaunchedEffect(Unit) {
            if (groupSession == null && !state.isRefreshing) viewModel.refresh()
            else viewModel.refreshGroupMembers()
        }
        BackHandler { viewModel.returnToSoloMap() }
        Box(Modifier.fillMaxSize()) {
            if (groupSession == null) {
                GroupSetupContent(
                    groupName = state.groupName,
                    visibility = state.groupVisibility,
                    publicGroups = state.publicGroups,
                    pendingGroupInvites = state.pendingGroupInvites,
                    isCreating = state.isGroupActionInProgress,
                    onGroupNameChange = viewModel::updateGroupName,
                    onVisibilityChange = viewModel::selectGroupVisibility,
                    onCreateGroup = viewModel::createGroup,
                    onJoinPublicGroup = viewModel::joinPublicGroup,
                    onAcceptInvite = viewModel::acceptGroupInvite,
                    onDeclineInvite = viewModel::declineGroupInvite,
                    onBack = viewModel::returnToSoloMap,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::selectSection,
                )
            } else {
                GroupModeContent(
                    groupSession = groupSession,
                    spots = state.groupSpots,
                    inviteText = state.inviteText,
                    invitableFriends = state.invitableFriends,
                    invitedGroupFriendIds = state.invitedGroupFriendIds,
                    isActionInProgress = state.isGroupActionInProgress,
                    onInviteTextChange = viewModel::updateInviteText,
                    onSendInvite = viewModel::sendGroupInvite,
                    onInviteFriend = viewModel::inviteFriendToGroup,
                    onLeaveGroup = viewModel::leaveGroup,
                    onBack = viewModel::returnToSoloMap,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::selectSection,
                    onSpotSelected = { spot -> viewingSpotPath = rootSpotPath(spot.id) }
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }
        return
    }

    if (state.selectedSection == HomeSection.Explore) {
        Column(Modifier.fillMaxSize()) {
            ExploreTabContent(
                accent = accent,
                spots = state.mapSpots,
                trendingCounts = state.trendingCounts,
                onSpotSelected = { viewingSpotPath = rootSpotPath(it) },
                onSuggestSpot = { showSubmitSpot = true },
                modifier = Modifier.weight(1f)
            )
            if (activeCheckIn != null) {
                ActiveSessionBar(
                    spotName = activeCheckIn.spot.name,
                    sessionStartTimeMillis = state.sessionStartTimeMillis,
                    onClick = viewModel::expandSession
                )
            }
            BottomNavigationShell(
                accent = accent,
                selectedSection = state.selectedSection,
                onSectionSelected = viewModel::selectSection
            )
        }
        return
    }

    if (state.selectedSection == HomeSection.Social) {
        SocialScreen(
            selectedTab = state.selectedSocialTab,
            onTabSelected = viewModel::selectSocialTab,
            selectedSection = state.selectedSection,
            onSectionSelected = viewModel::selectSection,
            friendRepository = friendRepository,
            loginStreak = loginStreak,
            activeSessionBar = if (activeCheckIn != null) {
                {
                    ActiveSessionBar(
                        spotName = activeCheckIn.spot.name,
                        sessionStartTimeMillis = state.sessionStartTimeMillis,
                        onClick = viewModel::expandSession
                    )
                }
            } else null
        )
        return
    }

    if (state.selectedSection == HomeSection.Profile) {
        Column(Modifier.fillMaxSize()) {
            ProfileTabContent(
                profileRepository = profileRepository,
                authRepository = authRepository,
                friendRepository = friendRepository,
                badgeRepository = badgeRepository,
                onSignOut = onSignOut,
                recentSessions = state.completedSessions,
                modifier = Modifier.weight(1f)
            )
            if (activeCheckIn != null) {
                ActiveSessionBar(
                    spotName = activeCheckIn.spot.name,
                    sessionStartTimeMillis = state.sessionStartTimeMillis,
                    onClick = viewModel::expandSession
                )
            }
            BottomNavigationShell(
                accent = accent,
                selectedSection = state.selectedSection,
                onSectionSelected = viewModel::selectSection
            )
        }
        return
    }

    val filteredMapSpots = remember(
        state.mapSpots, state.noiseFilter, state.lightingFilter, state.wifiFilter, state.spaceTypeFilter,
        state.amenityFilter, state.occupancyFilter
    ) {
        filterStudySpots(
            spots = state.mapSpots,
            filters = SpotFilters(
                noise = state.noiseFilter,
                lighting = state.lightingFilter,
                wifi = state.wifiFilter,
                spaceType = state.spaceTypeFilter,
                amenity = state.amenityFilter,
                maximumOccupancyPercent = state.occupancyFilter,
            )
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MapTabContent(
                    selectedMode = state.selectedMode,
                    mapSpots = filteredMapSpots,
                    allMapSpots = state.mapSpots,
                    selectedSpotId = state.selectedSpotId,
                    soloSpot = soloSpot,
                    accent = accent,
                    isRefreshing = state.isRefreshing,
                    noiseFilter = state.noiseFilter,
                    lightingFilter = state.lightingFilter,
                    wifiFilter = state.wifiFilter,
                    spaceTypeFilter = state.spaceTypeFilter,
                    amenityFilter = state.amenityFilter,
                    occupancyFilter = state.occupancyFilter,
                    onModeSelected = viewModel::selectMode,
                    onMapSpotSelected = viewModel::selectMapSpot,
                    onSpotSelected = { viewingSpotPath = rootSpotPath(it) },
                    onRefresh = viewModel::refresh,
                    onNoiseFilterChange = viewModel::setNoiseFilter,
                    onLightingFilterChange = viewModel::setLightingFilter,
                    onWifiFilterChange = viewModel::setWifiFilter,
                    onSpaceTypeFilterChange = viewModel::setSpaceTypeFilter,
                    onAmenityFilterChange = viewModel::setAmenityFilter,
                    onOccupancyFilterChange = viewModel::setOccupancyFilter,
                )
            }
            if (activeCheckIn != null) {
                ActiveSessionBar(
                    spotName = activeCheckIn.spot.name,
                    sessionStartTimeMillis = state.sessionStartTimeMillis,
                    onClick = viewModel::expandSession
                )
            }
            BottomNavigationShell(
                accent = accent,
                selectedSection = state.selectedSection,
                onSectionSelected = viewModel::selectSection
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
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
private fun HomeUnavailableScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Map,
            contentDescription = null,
            tint = SoloBlue,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Live places unavailable",
            color = Ink,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = BodyText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SoloBlue)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try again", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun BottomNavigationShell(
    accent: Color,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    val glassShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeBackground)
            .navigationBarsPadding()
            .padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = glassShape,
                    ambientColor = accent.copy(alpha = 0.10f),
                    spotColor = accent.copy(alpha = 0.16f)
                )
                .clip(glassShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFE9EEFB).copy(alpha = 0.78f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            accent.copy(alpha = 0.16f)
                        )
                    ),
                    shape = glassShape
                )
                .padding(5.dp),
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
    val color by animateColorAsState(
        targetValue = if (selected) accent else NavMuted,
        animationSpec = tween(durationMillis = 180),
        label = "bottomNavigationItemColor"
    )
    val itemShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .height(64.dp)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 7.dp,
                        shape = itemShape,
                        ambientColor = Color.White.copy(alpha = 0.85f),
                        spotColor = accent.copy(alpha = 0.16f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(itemShape)
            .background(
                brush = if (selected) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color.White.copy(alpha = 0.76f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                }
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                accent.copy(alpha = 0.10f)
                            )
                        ),
                        shape = itemShape
                    )
                } else {
                    Modifier
                }
            )
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
