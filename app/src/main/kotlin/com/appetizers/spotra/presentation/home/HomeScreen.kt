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
import androidx.compose.material.icons.rounded.Wifi
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
import com.appetizers.spotra.domain.model.SpotFeature
import com.appetizers.spotra.domain.model.SpotFeatureType
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

    var viewingSpotId by remember { mutableStateOf<String?>(null) }
    var reviewingSpotId by remember { mutableStateOf<String?>(null) }
    var editingReview by remember { mutableStateOf<Review?>(null) }
    var showSubmitSpot by remember { mutableStateOf(false) }

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
                onBack = { viewingSpotId = null },
                onSpaceSelected = { child ->
                    viewingSpotId = child.id
                }
            )
            return
        }

        Box(Modifier.fillMaxSize()) {
            SpotDetailScreen(
                spotId = spotId,
                accent = accent,
                onBack = { viewingSpotId = null },
                onCheckIn = { spot ->
                    viewModel.startCheckIn(spot, state.selectedMode) {
                        viewingSpotId = null
                    }
                },
                checkInLabel = if (state.selectedMode == StudyMode.Group) "Join with group" else "Start Session",
                homeRepository = homeRepository,
                reviewRepository = reviewRepository,
                friendRepository = friendRepository,
                onReview = {
                    editingReview = null
                    reviewingSpotId = spotId
                },
                onEditReview = { review ->
                    editingReview = review
                    reviewingSpotId = spotId
                },
                activeCheckInSpotId = activeCheckIn?.spot?.id,
                onEndSession = {
                    viewModel.checkOut()
                    viewingSpotId = null
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
        BackHandler { viewModel.returnToSoloMap() }
        Box(Modifier.fillMaxSize()) {
            if (groupSession == null) {
                GroupSetupContent(
                    groupName = state.groupName,
                    visibility = state.groupVisibility,
                    publicGroups = state.publicGroups,
                    isCreating = state.isGroupActionInProgress,
                    onGroupNameChange = viewModel::updateGroupName,
                    onVisibilityChange = viewModel::selectGroupVisibility,
                    onCreateGroup = viewModel::createGroup,
                    onJoinPublicGroup = viewModel::joinPublicGroup,
                    onBack = viewModel::returnToSoloMap,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::selectSection,
                )
            } else {
                GroupModeContent(
                    groupSession = groupSession,
                    spots = state.groupSpots,
                    inviteText = state.inviteText,
                    isActionInProgress = state.isGroupActionInProgress,
                    onInviteTextChange = viewModel::updateInviteText,
                    onSendInvite = viewModel::sendGroupInvite,
                    onLeaveGroup = viewModel::leaveGroup,
                    onBack = viewModel::returnToSoloMap,
                    selectedSection = state.selectedSection,
                    onSectionSelected = viewModel::selectSection,
                    onSpotSelected = { spot -> viewingSpotId = spot.id }
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
                onSpotSelected = { viewingSpotId = it },
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
                    onSpotSelected = { viewingSpotId = it },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTabContent(
    selectedMode: StudyMode,
    mapSpots: List<StudySpotSummary>,
    allMapSpots: List<StudySpotSummary>,
    selectedSpotId: String?,
    soloSpot: StudySpotSummary,
    accent: Color,
    isRefreshing: Boolean,
    noiseFilter: String?,
    lightingFilter: String?,
    wifiFilter: String?,
    spaceTypeFilter: StudyMode?,
    amenityFilter: String?,
    occupancyFilter: Int?,
    onModeSelected: (StudyMode) -> Unit,
    onMapSpotSelected: (String) -> Unit,
    onSpotSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onNoiseFilterChange: (String?) -> Unit,
    onLightingFilterChange: (String?) -> Unit,
    onWifiFilterChange: (String?) -> Unit,
    onSpaceTypeFilterChange: (StudyMode?) -> Unit,
    onAmenityFilterChange: (String?) -> Unit,
    onOccupancyFilterChange: (Int?) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        HomeHeader(
            selectedMode = selectedMode,
            accent = accent,
            onModeSelected = onModeSelected,
            activeFilterCount = listOf(
                noiseFilter,
                lightingFilter,
                wifiFilter,
                spaceTypeFilter,
                amenityFilter,
                occupancyFilter,
            ).count { it != null },
            onFilterClick = { showFilters = true },
        )
        CampusMap(
            spots = mapSpots,
            selectedSpotId = selectedSpotId,
            onSpotSelected = onMapSpotSelected,
            accent = accent,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        val displayedSpot = remember(allMapSpots, selectedSpotId, soloSpot) {
            allMapSpots.firstOrNull { it.id == selectedSpotId } ?: soloSpot
        }
        StudySpotCard(
            spot = displayedSpot,
            accent = accent,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp),
            onClick = { onSpotSelected(displayedSpot.id) }
        )
        Spacer(Modifier.height(12.dp))
    }

    if (showFilters) {
        SpotFiltersSheet(
            noise = noiseFilter,
            lighting = lightingFilter,
            wifi = wifiFilter,
            spaceType = spaceTypeFilter,
            amenity = amenityFilter,
            occupancy = occupancyFilter,
            accent = accent,
            onNoiseSelect = onNoiseFilterChange,
            onLightingSelect = onLightingFilterChange,
            onWifiSelect = onWifiFilterChange,
            onSpaceTypeSelect = onSpaceTypeFilterChange,
            onAmenitySelect = onAmenityFilterChange,
            onOccupancySelect = onOccupancyFilterChange,
            onClear = {
                onNoiseFilterChange(null)
                onLightingFilterChange(null)
                onWifiFilterChange(null)
                onSpaceTypeFilterChange(null)
                onAmenityFilterChange(null)
                onOccupancyFilterChange(null)
            },
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun GroupSetupContent(
    groupName: String,
    visibility: GroupVisibility,
    publicGroups: List<GroupStudySession>,
    isCreating: Boolean,
    onGroupNameChange: (String) -> Unit,
    onVisibilityChange: (GroupVisibility) -> Unit,
    onCreateGroup: () -> Unit,
    onJoinPublicGroup: (String) -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupModeContent(
    groupSession: GroupStudySession,
    spots: List<StudySpotSummary>,
    inviteText: String,
    isActionInProgress: Boolean,
    onInviteTextChange: (String) -> Unit,
    onSendInvite: () -> Unit,
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
                GroupSpotCard(
                    spot = spot,
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
        GroupInviteSheet(
            value = inviteText,
            onValueChange = onInviteTextChange,
            isSending = isActionInProgress,
            onSend = {
                onSendInvite()
                showInviteSheet = false
            },
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
private fun GroupSpotCard(spot: StudySpotSummary, onClick: () -> Unit) {
    val walkingLabel = studySpotDistanceLabel(spot.distanceMeters, spot.studyContextLabel)
    val (statusBackground, statusColor) = badgePillColors(spot.badge)
    val hoursLabel = remember(spot.operatingHours) {
        spot.operatingHours?.statusLabelAt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, GroupCardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "${spot.name}. Open group study options"
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(GroupSpotIconGreen, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = null,
                    tint = GroupGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (walkingLabel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(walkingLabel, color = BodyText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.background(statusBackground, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(noiseIcon(spot.badge), contentDescription = null, tint = statusColor, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(spot.badge, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Group, contentDescription = null, tint = GroupGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Good for groups", color = BodyText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (hoursLabel != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = BodyText, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(hoursLabel, color = BodyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupInviteSheet(
    value: String,
    onValueChange: (String) -> Unit,
    isSending: Boolean,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    val trimmedValue = value.trim()
    val emailLooksValid = trimmedValue.contains("@") && trimmedValue.substringAfterLast("@").contains(".")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Invite member", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Send an invitation to join this study session.", color = BodyText, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier.size(44.dp).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close invite", tint = Ink)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Email address", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, if (value.isNotBlank() && !emailLooksValid) ModerateFitText else DividerLine, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.MailOutline, contentDescription = null, tint = BodyText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isSending,
                    singleLine = true,
                    textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (value.isBlank()) Text("name@uwaterloo.ca", color = HeaderMuted, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            if (value.isNotBlank() && !emailLooksValid) {
                Spacer(Modifier.height(6.dp))
                Text("Enter a complete email address.", color = ModerateFitText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = onSend,
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

@Composable
private fun SocialScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCheckoutReviewSheet(
    spotName: String,
    sheetState: androidx.compose.material3.SheetState,
    onSubmit: (rating: Int, noiseLevel: String?, lighting: String?, wifiQuality: String?, occupancyPercent: Int?, comment: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(0) }
    var noiseIndex by remember { mutableStateOf<Int?>(null) }
    var lightingIndex by remember { mutableStateOf<Int?>(null) }
    var wifiIndex by remember { mutableStateOf<Int?>(null) }
    var occupancyIndex by remember { mutableStateOf<Int?>(null) }
    var comment by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(
                    text = "How was your session?",
                    color = Ink,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(androidx.compose.ui.Modifier.height(4.dp))
                Text(text = spotName, color = HeaderMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Rate $star stars",
                        tint = if (star <= rating) StarGold else Color(0xFFE0DDDA),
                        modifier = androidx.compose.ui.Modifier
                            .size(40.dp)
                            .clickable { rating = star }
                    )
                }
            }
            LabelSlider(
                label = "Noise level",
                options = listOf("Silent", "Low", "Moderate", "Lively"),
                selectedIndex = noiseIndex,
                onSelect = { noiseIndex = it },
            )
            LabelSlider(
                label = "Lighting",
                options = listOf("Poor", "Good", "Bright", "Natural"),
                selectedIndex = lightingIndex,
                onSelect = { lightingIndex = it },
            )
            LabelSlider(
                label = "WiFi quality",
                options = listOf("Poor", "OK", "Good", "Fast"),
                selectedIndex = wifiIndex,
                onSelect = { wifiIndex = it },
            )
            LabelSlider(
                label = "How busy was it?",
                options = listOf("Empty", "Some", "Busy", "Packed"),
                selectedIndex = occupancyIndex,
                onSelect = { occupancyIndex = it },
            )
            BasicTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .background(HomeBackground, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (comment.isEmpty()) {
                        Text("Any tips for other students? (optional)", color = HeaderMuted, fontSize = 15.sp)
                    }
                    inner()
                }
            )
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) { Text("Skip") }
                Button(
                    onClick = {
                        if (rating > 0) onSubmit(
                            rating,
                            noiseLabel(noiseIndex),
                            lightingLabel(lightingIndex),
                            wifiLabel(wifiIndex),
                            occupancyToPercent(occupancyIndex),
                            comment.ifBlank { null }
                        )
                    },
                    enabled = rating > 0,
                    modifier = androidx.compose.ui.Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SoloBlue)
                ) { Text("Submit") }
            }
        }
    }
}

@Composable
private fun LiveCheckInScreen(
    session: CheckInSession,
    sessionStartTimeMillis: Long,
    accent: Color,
    requestedBuddyIds: Set<String>,
    onBuddyRequest: (String) -> Unit,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    var selectedBuddy by remember(session.id) { mutableStateOf<CheckedInStudent?>(null) }

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
                sessionStartTimeMillis = sessionStartTimeMillis,
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
    Box(modifier = modifier.fillMaxWidth().background(Color.White)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            contentPadding = PaddingValues(
                start = 36.dp, top = 20.dp, end = 24.dp, bottom = 104.dp
            )
        ) {
            item {
                Text(text = "WHO'S HERE", color = SectionLabel, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
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
                itemsIndexed(items = students, key = { _, s -> s.id }) { index, student ->
                    CheckedInStudentRow(
                        student = student,
                        requested = student.id in requestedBuddyIds,
                        onBuddyClick = { onBuddyClick(student) }
                    )
                    if (index < students.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(DividerLine))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
        )
    }
}

@Composable
private fun LiveCheckInHeader(spotName: String, peopleHere: Int, onBack: () -> Unit) {
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
            Box(Modifier.size(10.dp).background(CheckedInDot, CircleShape))
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
private fun CheckedInStudentRow(student: CheckedInStudent, requested: Boolean, onBuddyClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (student.isSelf) SelfRowBackground else Color.White)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(avatarColorFor(student.id), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = student.initials, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
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
                    Text(text = "friend", color = SoloBlue, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
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
            else -> BuddyRequestButton(requested = requested, onClick = onBuddyClick)
        }
    }
}

@Composable
private fun RelationPill(text: String, contentColor: Color, backgroundColor: Color) {
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
                text = student.detail,
                color = HeaderMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Send a buddy request to connect after this study session.",
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .background(SwitcherTrack, RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Decline",
                        color = HeaderMuted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                val alreadySent = requested
                val theyRequested = student.hasSentMeRequest
                val buttonLabel = when {
                    alreadySent -> "Sent"
                    theyRequested -> "Accept their request"
                    else -> "Send buddy request"
                }
                val buttonIcon = if (alreadySent) Icons.Rounded.Check else Icons.Rounded.PersonAdd
                Row(
                    modifier = Modifier
                        .weight(1.45f)
                        .height(54.dp)
                        .background(if (alreadySent) RequestedPill else SoloBlue, RoundedCornerShape(16.dp))
                        .clickable(enabled = !alreadySent, onClick = onAccept),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = null,
                        tint = if (alreadySent) QuietText else Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buttonLabel,
                        color = if (alreadySent) QuietText else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionBar(
    spotName: String,
    sessionStartTimeMillis: Long,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CheckInHeader)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).background(CheckedInDot, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Session · $spotName",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        SessionElapsedText(
            sessionStartTimeMillis = sessionStartTimeMillis,
            color = CheckedInText,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Return ›",
            color = HeaderSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CheckInSessionPanel(sessionStartTimeMillis: Long, onCheckout: () -> Unit) {
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
            Text(text = "Session time", color = BodyText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            SessionElapsedText(
                sessionStartTimeMillis = sessionStartTimeMillis,
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
            Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = BodyText, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = "Check out & review", color = BodyText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SessionElapsedText(
    sessionStartTimeMillis: Long,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight
) {
    var elapsedSeconds by remember(sessionStartTimeMillis) {
        mutableIntStateOf(((System.currentTimeMillis() - sessionStartTimeMillis) / 1000).toInt())
    }
    LaunchedEffect(sessionStartTimeMillis) {
        while (true) {
            delay(1000)
            elapsedSeconds = ((System.currentTimeMillis() - sessionStartTimeMillis) / 1000).toInt()
        }
    }
    Text(
        text = elapsedSeconds.asSessionTime(),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
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
            drawLine(SoloBlue, center, Offset(center.x, center.y - size.height * .26f), stroke, StrokeCap.Round)
            drawLine(SoloBlue, center, Offset(center.x + size.width * .22f, center.y + size.height * .12f), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun HomeHeader(
    selectedMode: StudyMode,
    accent: Color,
    onModeSelected: (StudyMode) -> Unit,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeBackground)
            .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 12.dp)
    ) {
        HomeBrandHeader(
            accent = accent,
        )
        Spacer(Modifier.height(12.dp))
        ModeSwitcher(selectedMode = selectedMode, accent = accent, onModeSelected = onModeSelected)
        Spacer(Modifier.height(10.dp))
        FilterControl(
            activeFilterCount = activeFilterCount,
            accent = accent,
            onClick = onFilterClick
        )
    }
}

@Composable
private fun HomeBrandHeader(
    accent: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        SpotraLandingWordmark(accent)
    }
}

private val NOISE_FILTER_OPTIONS = listOf("Silent", "Low", "Moderate", "Lively")
private val LIGHTING_FILTER_OPTIONS = listOf("Poor", "Good", "Bright", "Natural")
private val WIFI_FILTER_OPTIONS = listOf("Poor", "OK", "Good", "Fast")
private val AMENITY_OPTIONS = listOf("Wi-Fi", "Outlets", "Whiteboard", "Accessible")
private val OCCUPANCY_OPTIONS = listOf(50 to "< 50% full", 75 to "< 75% full")

@Composable
private fun FilterControl(activeFilterCount: Int, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, DividerLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (activeFilterCount == 0) "Filters, none active" else "Filters, $activeFilterCount active"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Filters",
            color = Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (activeFilterCount == 0) "All spots" else "$activeFilterCount active",
            color = accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(22.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotFiltersSheet(
    noise: String?,
    lighting: String?,
    wifi: String?,
    spaceType: StudyMode?,
    amenity: String?,
    occupancy: Int?,
    accent: Color,
    onNoiseSelect: (String?) -> Unit,
    onLightingSelect: (String?) -> Unit,
    onWifiSelect: (String?) -> Unit,
    onSpaceTypeSelect: (StudyMode?) -> Unit,
    onAmenitySelect: (String?) -> Unit,
    onOccupancySelect: (Int?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filter study spots",
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Clear all",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onClear),
                )
            }
            FilterSection("Noise") {
                NoiseFilterChips(noise, accent, onNoiseSelect)
            }
            FilterSection("Lighting") {
                StringFilterChips("Any lighting", LIGHTING_FILTER_OPTIONS, lighting, accent, onLightingSelect)
            }
            FilterSection("Wi-Fi") {
                StringFilterChips("Any Wi-Fi", WIFI_FILTER_OPTIONS, wifi, accent, onWifiSelect)
            }
            FilterSection("Space type") {
                SpaceTypeFilterChips(spaceType, accent, onSpaceTypeSelect)
            }
            FilterSection("Amenity") {
                AmenityFilterChips(amenity, accent, onAmenitySelect)
            }
            FilterSection("Occupancy") {
                OccupancyFilterChips(occupancy, accent, onOccupancySelect)
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Show results", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun FilterSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        content()
    }
}

@Composable
private fun StringFilterChips(
    allLabel: String,
    options: List<String>,
    selected: String?,
    accent: Color,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        item {
            FilterChip(allLabel, selected == null, accent) { onSelect(null) }
        }
        items(options) { option ->
            FilterChip(option, selected == option, accent) {
                onSelect(if (selected == option) null else option)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoiseFilterSheet(
    selected: String?,
    accent: Color,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
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
                text = "Noise level",
                color = Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Choose the environment that helps you focus.",
                color = BodyText,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(18.dp))
            val choices = listOf<String?>(null) + NOISE_FILTER_OPTIONS
            choices.forEach { option ->
                val isSelected = selected == option
                val label = option ?: "Any noise"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            if (isSelected) accent.copy(alpha = 0.10f) else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = noiseIcon(label),
                        contentDescription = null,
                        tint = if (isSelected) accent else BodyText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 17.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private fun noiseIcon(label: String): ImageVector = when (label.lowercase()) {
    "silent", "quiet" -> Icons.AutoMirrored.Rounded.VolumeOff
    else -> Icons.AutoMirrored.Rounded.VolumeDown
}

@Composable
private fun NoiseFilterChips(selected: String?, accent: Color, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            FilterChip(label = "All noise", selected = selected == null, accent = accent, onClick = { onSelect(null) })
        }
        items(NOISE_FILTER_OPTIONS) { option ->
            FilterChip(label = option, selected = selected == option, accent = accent, onClick = {
                onSelect(if (selected == option) null else option)
            })
        }
    }
}

@Composable
private fun SpaceTypeFilterChips(selected: StudyMode?, accent: Color, onSelect: (StudyMode?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            FilterChip(label = "All spaces", selected = selected == null, accent = accent, onClick = { onSelect(null) })
        }
        item {
            FilterChip(label = "Solo", selected = selected == StudyMode.Solo, accent = accent, onClick = {
                onSelect(if (selected == StudyMode.Solo) null else StudyMode.Solo)
            })
        }
        item {
            FilterChip(label = "Group", selected = selected == StudyMode.Group, accent = accent, onClick = {
                onSelect(if (selected == StudyMode.Group) null else StudyMode.Group)
            })
        }
    }
}

@Composable
private fun AmenityFilterChips(selected: String?, accent: Color, onSelect: (String?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            FilterChip(label = "All amenities", selected = selected == null, accent = accent, onClick = { onSelect(null) })
        }
        items(AMENITY_OPTIONS) { option ->
            FilterChip(label = option, selected = selected == option, accent = accent, onClick = {
                onSelect(if (selected == option) null else option)
            })
        }
    }
}

@Composable
private fun OccupancyFilterChips(selected: Int?, accent: Color, onSelect: (Int?) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            FilterChip(label = "Any occupancy", selected = selected == null, accent = accent, onClick = { onSelect(null) })
        }
        items(OCCUPANCY_OPTIONS) { (threshold, label) ->
            FilterChip(label = label, selected = selected == threshold, accent = accent, onClick = {
                onSelect(if (selected == threshold) null else threshold)
            })
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .background(if (selected) accent else HomeBackground, RoundedCornerShape(20.dp))
            .border(1.dp, if (selected) accent else DividerLine, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        color = if (selected) Color.White else BodyText,
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

@Composable
private fun SpotraLandingWordmark(accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "sp", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = "o", color = accent, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
        Text(text = "tra", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ModeSwitcher(selectedMode: StudyMode, accent: Color, onModeSelected: (StudyMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SwitcherTrack, RoundedCornerShape(24.dp))
            .padding(4.dp),
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
    val bgColor by animateColorAsState(
        targetValue = if (selected) selectedColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "segment-bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else MutedText,
        animationSpec = tween(durationMillis = 200),
        label = "segment-content"
    )
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CampusMap(
    spots: List<StudySpotSummary>,
    selectedSpotId: String?,
    onSpotSelected: (String) -> Unit,
    accent: Color,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val located = remember(spots) { spots.filter { it.latitude != null && it.longitude != null } }
    val displayState = mapDisplayState(
        tokenBlank = BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank(),
        locatedCount = located.size
    )

    if (displayState == MapDisplayState.PLACEHOLDER) {
        CampusMapPlaceholder(modifier = modifier)
        return
    }


    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(MapConfig.CAMPUS_LNG, MapConfig.CAMPUS_LAT))
            zoom(MapConfig.DEFAULT_ZOOM)
        }
    }

    val zoomBy: (Double) -> Unit = { delta ->
        val cameraState = mapViewportState.cameraState
        val nextZoom = MapConfig.coerceZoom((cameraState?.zoom ?: MapConfig.DEFAULT_ZOOM) + delta)
        mapViewportState.easeTo(
            cameraOptions {
                center(cameraState?.center ?: Point.fromLngLat(MapConfig.CAMPUS_LNG, MapConfig.CAMPUS_LAT))
                zoom(nextZoom)
                bearing(cameraState?.bearing ?: 0.0)
                pitch(cameraState?.pitch ?: 0.0)
            },
            MapAnimationOptions.mapAnimationOptions { duration(180) }
        )
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
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
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
                            color = occupancyPinColor(spot),
                            selected = spot.id == selectedSpotId,
                            contentDescription = "${spot.name}, occupancy ${spot.badge}",
                            modifier = Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSpotSelected(spot.id) }
                        )
                    }
                }
            }

        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapCircleButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Zoom in",
                onClick = { zoomBy(1.0) }
            )
            MapCircleButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Zoom out",
                onClick = { zoomBy(-1.0) }
            )
            MapCircleButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh spot occupancy",
                loading = isRefreshing,
                onClick = onRefresh
            )
        }

        if (displayState == MapDisplayState.EMPTY) {
            MapEmptyOverlay(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MapEmptyOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Map,
            contentDescription = null,
            tint = HeaderMuted,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "No study spots to show",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tap refresh to try again",
            color = BodyText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MapCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape, clip = false)
            .background(Color.White, CircleShape)
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = SoloBlue
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CampusMapPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().background(MapBackground)) {
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
                drawRoundRect(color = blockColor, topLeft = topLeft, size = Size(blockWidth, blockHeight), cornerRadius = CornerRadius(blockRadius, blockRadius))
            }
            val streetWidth = 13.dp.toPx()
            listOf(.26f, .58f, .91f).forEach { x ->
                drawLine(Color.White, Offset(size.width * x, 0f), Offset(size.width * x, size.height), streetWidth, StrokeCap.Round)
            }
            listOf(.31f, .59f, .87f).forEach { y ->
                drawLine(Color.White, Offset(0f, size.height * y), Offset(size.width, size.height * y), streetWidth, StrokeCap.Round)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null, tint = HeaderMuted, modifier = Modifier.size(28.dp))
            Text("Map unavailable", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Live places are listed below", color = BodyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MapPin(
    label: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String = label
) {
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "pin-label-alpha"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pin-dot-scale"
    )

    val pillShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier
                .graphicsLayer { alpha = labelAlpha }
                .shadow(6.dp, pillShape, clip = false)
                .background(color, pillShape)
                .border(2.dp, Color.White, pillShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                .size(12.dp)
                .shadow(3.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
                .padding(2.5.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
private fun BuildingSpacesScreen(
    parentSpot: StudySpotSummary,
    homeRepository: HomeRepository,
    accent: Color,
    onBack: () -> Unit,
    onSpaceSelected: (StudySpotDetail) -> Unit
) {
    var spaces by remember(parentSpot.id) { mutableStateOf<List<StudySpotDetail>>(emptyList()) }
    var isLoading by remember(parentSpot.id) { mutableStateOf(true) }
    var error by remember(parentSpot.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(parentSpot.id) {
        isLoading = true
        error = null
        runCatching { homeRepository.childSpots(parentSpot.id) }
            .onSuccess { spaces = it }
            .onFailure { error = it.toUserMessage("Could not load study spaces.") }
        isLoading = false
    }

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
                .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(HomeBackground, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Ink,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = parentSpot.name,
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${parentSpot.childCount} study spaces",
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        when (buildingSpacesDisplayState(isLoading = isLoading, error = error, spaceCount = spaces.size)) {
            BuildingSpacesDisplayState.LOADING -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            BuildingSpacesDisplayState.ERROR -> {
                Text(
                    text = error.orEmpty(),
                    modifier = Modifier.padding(24.dp),
                    color = DPAtriumRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            BuildingSpacesDisplayState.EMPTY -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No study spaces listed for this building yet.",
                        modifier = Modifier.padding(24.dp),
                        color = BodyText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            BuildingSpacesDisplayState.CONTENT -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    spaces
                        .groupBy { it.floor ?: "Building" }
                        .forEach { (floor, floorSpaces) ->
                            item { SectionHeader(label = floor) }
                            itemsIndexed(
                                items = floorSpaces,
                                key = { _, space -> space.id }
                            ) { _, space ->
                                BuildingSpaceRow(
                                    space = space,
                                    accent = accent,
                                    onClick = { onSpaceSelected(space) }
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun BuildingSpaceRow(
    space: StudySpotDetail,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.5.dp, DividerLine, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (space.studyContextLabel == "Group-friendly") Icons.Rounded.Group else Icons.Rounded.School,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = space.name,
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                space.capacity?.let { capacity ->
                    Text(
                        text = "Capacity $capacity",
                        color = HeaderMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (space.capacity != null && space.studyContextLabel != null) {
                    Text(text = " - ", color = HeaderMuted, fontSize = 14.sp)
                }
                space.studyContextLabel?.let { label ->
                    Text(
                        text = label,
                        color = HeaderMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = space.occupancyPercent?.let { "$it%" } ?: "0%",
                color = if ((space.occupancyPercent ?: 0) > 70) DPAtriumRed else GroupGreen,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "live full",
                color = HeaderMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = NavMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun StudySpotCard(
    spot: StudySpotSummary,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = onClick?.let { action ->
        Modifier
            .clickable(onClick = action)
            .semantics {
                role = Role.Button
                contentDescription = if (spot.childCount > 0) {
                    "${spot.name}. Open available study spaces"
                } else {
                    "${spot.name}. Open place details"
                }
            }
    } ?: Modifier
    val walkingLabel = studySpotDistanceLabel(spot.distanceMeters, spot.studyContextLabel)
    val statusIcon = noiseIcon(spot.badge)
    val hoursLabel = remember(spot.operatingHours) {
        spot.operatingHours?.statusLabelAt()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(1.dp, DividerLine, RoundedCornerShape(22.dp))
            .then(clickModifier)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Spots without a curated photo keep the original map glyph rather than showing an
            // empty placeholder tile.
            if (spot.photoUrl != null) {
                SpotThumbnail(photoUrl = spot.photoUrl, size = 54.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(CardBackground, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Rounded.Map, contentDescription = null, tint = accent, modifier = Modifier.size(27.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    color = Ink,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (walkingLabel.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = walkingLabel,
                        color = BodyText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(26.dp)
            )
        }
        if (hoursLabel != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = BodyText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = hoursLabel,
                    color = BodyText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        val (badgeBg, badgeFg) = badgePillColors(spot.badge)
        Row(
            modifier = Modifier
                .background(badgeBg, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(statusIcon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = spot.badge,
                color = badgeFg,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (spot.childCount > 0) "View study spaces" else "View place details",
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

internal fun studySpotDistanceLabel(distanceMeters: Int?, fallback: String?): String {
    val meters = distanceMeters?.takeIf { it >= 0 } ?: return fallback.orEmpty()
    return when {
        meters <= 2_000 -> "${kotlin.math.round(meters / 85.0).toInt().coerceAtLeast(1)} min walk"
        meters <= 10_000 -> "${"%.1f".format(java.util.Locale.US, meters / 1_000.0)} km away"
        meters <= 50_000 -> "${kotlin.math.round(meters / 1_000.0).toInt()} km away"
        else -> fallback.orEmpty()
    }
}

private fun badgePillColors(badge: String): Pair<Color, Color> = when (badge.lowercase()) {
    "silent", "quiet" -> QuietPill to QuietText
    "moderate" -> ModerateFitBackground to ModerateFitText
    "busy", "lively" -> Color(0xFFFFECEC) to Color(0xFFB91C1C)
    else -> SwitcherTrack to BodyText
}

@Composable
private fun BottomNavigationShell(
    accent: Color,
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
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
                .shadow(12.dp, RoundedCornerShape(28.dp), clip = false)
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 8.dp),
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
    val color = if (selected) accent else NavMuted
    Column(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(if (selected) accent else Color.Transparent, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(6.dp))
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private data class SocialPerson(
    val id: String,
    val initials: String,
    val name: String,
    val detail: String,
    val active: Boolean = true,
    val streakDays: Int? = null,
)

private fun occupancyPinColor(spot: StudySpotSummary): Color =
    spot.occupancyPercent?.let(::occupancyPinColorFromPercent) ?: HeaderMuted

private fun occupancyPinColorFromPercent(percent: Int): Color = when {
    percent >= 75 -> DPAtriumRed
    percent >= 50 -> SLCOrange
    else -> LibraryGreen
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
