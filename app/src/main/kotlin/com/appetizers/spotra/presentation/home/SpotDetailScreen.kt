package com.appetizers.spotra.presentation.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.SpotOccupancy
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.model.withOccupancy
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.presentation.toUserMessage
import java.util.Locale
import kotlinx.coroutines.launch

private enum class ReviewFilter { Friends, All }

private val DetailBackground = Color(0xFFF7F7F4)
private val DetailCardBorder = Color(0xFFE0E3E8)
private val DetailFilterBackground = Color(0xFFF2F3F5)
private val DetailTeal = Color(0xFF0A8A76)
private val DetailTealDark = Color(0xFF066B5D)
private val DetailTealSoft = Color(0xFFE5F5F1)
private val DetailTealBorder = Color(0xFF93CFC2)
private val DetailBlueSoft = Color(0xFFEEF3FF)
private val DetailBlueBorder = Color(0xFFB8C8F4)
private val DetailAmber = Color(0xFF9A6700)

@Composable
internal fun SpotDetailScreen(
    spotId: String,
    accent: Color,
    onBack: () -> Unit,
    onCheckIn: (StudySpotSummary) -> Unit,
    checkInLabel: String = "Start Session",
    homeRepository: HomeRepository,
    reviewRepository: ReviewRepository,
    friendRepository: FriendRepository? = null,
    liveOccupancy: SpotOccupancy? = null,
    onReview: () -> Unit,
    onEditReview: (Review) -> Unit = {},
    activeCheckInSpotId: String? = null,
    activeCheckInSpotName: String? = null,
    onEndSession: () -> Unit = {}
) {
    val sessionActiveHere = activeCheckInSpotId == spotId
    val sessionActiveElsewhere = activeCheckInSpotId != null && !sessionActiveHere

    var spot by remember(spotId) { mutableStateOf<StudySpotDetail?>(null) }
    var reviews by remember(spotId) { mutableStateOf<List<Review>>(emptyList()) }
    var reviewLoadError by remember(spotId) { mutableStateOf<String?>(null) }
    var friendsAtSpot by remember(spotId) { mutableStateOf<List<FriendProfile>>(emptyList()) }
    var acceptedFriendIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selfId by remember { mutableStateOf<String?>(null) }
    var reviewFilter by remember { mutableStateOf(ReviewFilter.All) }
    var error by remember(spotId) { mutableStateOf<String?>(null) }
    var pendingDeleteReview by remember { mutableStateOf<Review?>(null) }
    val scope = rememberCoroutineScope()
    val currentLiveOccupancy by rememberUpdatedState(liveOccupancy)

    LaunchedEffect(spotId) {
        error = null
        reviewLoadError = null
        runCatching { homeRepository.spotDetail(spotId) }
            .onSuccess { loaded ->
                spot = currentLiveOccupancy?.let(loaded::withOccupancy) ?: loaded
            }
            .onFailure { error = it.toUserMessage("Could not load this place.") }

        runCatching { reviewRepository.reviewsFor(spotId) }
            .onSuccess { reviews = it }
            .onFailure { reviewLoadError = "Could not load reviews." }
        if (friendRepository != null) {
            friendsAtSpot = runCatching { friendRepository.fetchFriendsAtSpot(spotId) }.getOrDefault(emptyList())
            val profiles = runCatching { friendRepository.fetchFriendProfiles() }.getOrNull()
            selfId = runCatching { friendRepository.currentUserId() }.getOrNull()
            acceptedFriendIds = profiles?.filter { it.isAccepted }?.map { it.id }?.toSet() ?: emptySet()
        }
    }

    LaunchedEffect(liveOccupancy) {
        liveOccupancy?.let { occupancy ->
            spot = spot?.withOccupancy(occupancy)
            if (friendRepository != null) {
                friendsAtSpot = runCatching {
                    friendRepository.fetchFriendsAtSpot(spotId)
                }.getOrDefault(friendsAtSpot)
            }
        }
    }

    val displayedReviews = when (reviewFilter) {
        ReviewFilter.Friends -> reviews.filter { it.reviewerId == selfId || it.reviewerId in acceptedFriendIds }
        ReviewFilter.All -> reviews
    }

    BackHandler(onBack = onBack)

    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        insetsController?.isAppearanceLightStatusBars = false

        onDispose {
            previousLightStatusBars?.let { wasLight ->
                insetsController?.isAppearanceLightStatusBars = wasLight
            }
        }
    }

    val detail = spot
    if (detail == null) {
        SpotDetailLoadingOrError(
            message = error,
            onBack = onBack
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CheckInHeader)
    ) {
        SpotDetailHeader(
            spot = detail,
            friendsStudying = friendsAtSpot,
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DetailBackground),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { SpotAvailabilitySummary(spot = detail) }
            item { SpotSignalCard(spot = detail) }
            item { SpotAmenitiesSection(amenities = detail.amenities) }
            detail.bookingUrl?.let { url ->
                item { BookRoomBanner(url = url) }
            }
            item {
                SpotReviewsSection(
                    reviews = displayedReviews,
                    reviewFilter = reviewFilter,
                    onFilterChange = { reviewFilter = it },
                    showFilter = friendRepository != null,
                    loadError = reviewLoadError,
                    onEditReview = onEditReview,
                    onDeleteReview = { pendingDeleteReview = it },
                )
            }
        }
        SpotActionButtons(
            accent = accent,
            sessionActiveHere = sessionActiveHere,
            sessionActiveElsewhere = sessionActiveElsewhere,
            activeCheckInSpotName = activeCheckInSpotName,
            checkInLabel = checkInLabel,
            onCheckIn = { onCheckIn(detail.toSummary()) },
            onEndSession = onEndSession,
            onReview = onReview
        )
    }

    pendingDeleteReview?.let { review ->
        AlertDialog(
            onDismissRequest = { pendingDeleteReview = null },
            title = { Text("Delete review?") },
            text = { Text("This removes your review from this study spot.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteReview = null
                        scope.launch {
                            runCatching { reviewRepository.delete(review.id) }
                                .onSuccess { reviews = reviews.filterNot { it.id == review.id } }
                                .onFailure { reviewLoadError = it.toUserMessage("Could not delete your review.") }
                        }
                    }
                ) {
                    Text("Delete", color = DPAtriumRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteReview = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SpotDetailLoadingOrError(message: String?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(22.dp)
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
        Spacer(Modifier.height(28.dp))
        Text(
            text = message ?: "Loading spot...",
            color = if (message == null) HeaderMuted else DPAtriumRed,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun SpotDetailHeader(
    spot: StudySpotDetail,
    friendsStudying: List<FriendProfile>,
    onBack: () -> Unit
) {
    val photos = spot.photos
    val hasPhotos = photos.isNotEmpty()
    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasPhotos) Modifier.heightIn(min = 260.dp) else Modifier)
            .background(CheckInHeader)
    ) {
        if (hasPhotos) {
            SpotPhotoBackdrop(
                photos = photos,
                spotName = spot.name,
                pagerState = pagerState,
                modifier = Modifier.matchParentSize()
            )
            // Pinned to the top so the arrow never floats in the middle of a tall photo.
            HeaderBackButton(
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 22.dp, top = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                // With a photo the image runs full-bleed behind the status bar and the back
                // button layer above carries the inset instead.
                .then(if (hasPhotos) Modifier else Modifier.statusBarsPadding())
                .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 24.dp)
        ) {
            if (!hasPhotos) {
                HeaderBackButton(onBack = onBack)
                Spacer(Modifier.height(18.dp))
            }

            val contextLabel = spot.badge
                .takeIf { it.isNotBlank() }
                ?.uppercase(Locale.US)
            if (contextLabel != null) {
                Text(
                    text = contextLabel,
                    color = HeaderSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = spot.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(4.dp))

            val locationLabel = listOfNotNull(
                spot.building,
                spot.floor,
                spot.distanceMeters?.let { "${it}m away" }
            ).filter { it.isNotBlank() }.joinToString(" · ")

            Text(
                text = locationLabel,
                color = HeaderSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )

            if (spot.isLive) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).background(CheckedInDot, CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "${spot.peopleHere} ${if (spot.peopleHere == 1) "person" else "people"} checked in now",
                        color = CheckedInText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (friendsStudying.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        friendsStudying.take(3).forEach { friend ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(avatarColorFor(friend.id), CircleShape)
                                    .border(2.dp, CheckInHeader, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = friend.initials,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = friendsStudying.friendStatusLabel(),
                        color = HeaderSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (photos.size > 1) {
                Spacer(Modifier.height(14.dp))
                SpotPhotoDots(
                    count = photos.size,
                    selectedIndex = pagerState.currentPage
                )
            }
        }
    }
}

@Composable
private fun HeaderBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(HeaderButton, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SpotAvailabilitySummary(spot: StudySpotDetail) {
    val occupancy = spot.occupancyPercent?.coerceIn(0, 100)
    val occupancyLabel = occupancy?.let { "$it%" } ?: "—"
    val availabilityLabel = when {
        occupancy == null -> "Occupancy unavailable"
        occupancy < 50 -> "Plenty of room"
        occupancy < 75 -> "Filling up"
        else -> "Nearly full"
    }
    val occupancyDetail = occupancy?.let { "$it% full" } ?: "No recent occupancy report"
    val ratingLabel = spot.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "New"
    val ratingDescription = spot.rating?.let { "${String.format(Locale.US, "%.1f", it)} out of 5 rating" }
        ?: "No ratings yet"
    val summaryDescription = listOfNotNull(
        availabilityLabel,
        occupancyDetail,
        "Live occupancy data".takeIf { spot.occupancyPercentIsLive },
        ratingDescription
    ).joinToString(separator = ". ", postfix = ".")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = summaryDescription
            },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DetailCardBorder),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (occupancy ?: 0) / 100f },
                    modifier = Modifier.size(82.dp),
                    color = if (occupancy == null) DividerLine else DetailTeal,
                    trackColor = DetailTealSoft,
                    strokeWidth = 8.dp
                )
                Text(
                    text = occupancyLabel,
                    color = if (occupancy == null) HeaderMuted else DetailTealDark,
                    fontSize = if (occupancy == null) 24.sp else 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = availabilityLabel,
                    color = Ink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = occupancyDetail,
                    color = HeaderMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(58.dp)
                    .background(DetailCardBorder)
            )
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .padding(start = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = ratingLabel,
                    color = Ink,
                    fontSize = if (spot.rating == null) 17.sp else 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "RATING",
                    color = SectionLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SpotSignalCard(spot: StudySpotDetail) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 12.dp, end = 20.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DetailCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SignalItem(
                icon = Icons.Rounded.GraphicEq,
                value = spot.noiseLevel ?: "No data",
                label = "Noise",
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(78.dp)
                    .background(DetailCardBorder)
            )
            SignalItem(
                icon = Icons.Rounded.LightMode,
                value = spot.lighting ?: "No data",
                label = "Lighting",
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(78.dp)
                    .background(DetailCardBorder)
            )
            SignalItem(
                icon = Icons.Rounded.Wifi,
                value = spot.wifiQuality ?: "No data",
                label = "Wi-Fi",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SignalItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = "$label: $value"
            }
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(DetailFilterBackground, CircleShape)
                .border(1.5.dp, DetailCardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label.uppercase(Locale.getDefault()),
            color = SectionLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpotAmenitiesSection(amenities: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 24.dp, end = 20.dp)
    ) {
        Text(
            text = "Amenities",
            color = Ink,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(12.dp))
        if (amenities.isEmpty()) {
            Text(
                text = "No amenities listed.",
                color = HeaderMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                amenities.forEach { amenity ->
                    AmenityChip(amenity = amenity)
                }
            }
        }
    }
}

@Composable
private fun AmenityChip(amenity: String) {
    val icon = when {
        amenity.contains("double", ignoreCase = true) ||
            amenity.contains("group", ignoreCase = true) -> Icons.Rounded.Groups
        amenity.contains("single", ignoreCase = true) ||
            amenity.contains("individual", ignoreCase = true) -> Icons.Rounded.Person
        else -> Icons.Rounded.CheckCircle
    }
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, DetailCardBorder, RoundedCornerShape(18.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "Amenity: $amenity"
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SectionLabel,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = amenity,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReviewFilterControl(
    selectedFilter: ReviewFilter,
    onFilterChange: (ReviewFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(DetailFilterBackground, RoundedCornerShape(18.dp))
            .border(1.dp, DetailCardBorder, RoundedCornerShape(18.dp))
            .padding(2.dp)
    ) {
        ReviewFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .background(
                        if (isSelected) SoloBlue else Color.Transparent,
                        RoundedCornerShape(15.dp)
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onFilterChange(filter) }
                    )
                    .semantics {
                        selected = isSelected
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = filter.name,
                    color = if (isSelected) Color.White else SectionLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SpotReviewsSection(
    reviews: List<Review>,
    reviewFilter: ReviewFilter = ReviewFilter.All,
    onFilterChange: (ReviewFilter) -> Unit = {},
    showFilter: Boolean = false,
    loadError: String? = null,
    onEditReview: (Review) -> Unit = {},
    onDeleteReview: (Review) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 18.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent reviews",
                color = Ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (showFilter) {
                ReviewFilterControl(
                    selectedFilter = reviewFilter,
                    onFilterChange = onFilterChange
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            loadError != null -> EmptyReviewCard(
                message = loadError,
                textColor = DPAtriumRed
            )
            reviews.isEmpty() -> EmptyReviewCard(
                message = if (reviewFilter == ReviewFilter.Friends) {
                    "No friend reviews yet."
                } else {
                    "No reviews yet. Be the first to share what this space is like."
                }
            )
            else -> reviews.forEachIndexed { index, review ->
                ReviewRow(
                    review = review,
                    onEdit = { onEditReview(review) },
                    onDelete = { onDeleteReview(review) },
                )
                if (index < reviews.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyReviewCard(
    message: String,
    textColor: Color = HeaderMuted,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DetailCardBorder)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ReviewRow(
    review: Review,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, DetailCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(avatarColorFor(review.reviewerId), CircleShape)
                    .semantics {
                        contentDescription = "${review.reviewerName}'s avatar"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = review.reviewerInitials,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = review.reviewerName,
                        color = Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StarRow(rating = review.rating)
                }
                review.comment
                    ?.takeIf { it.isNotBlank() }
                    ?.let { comment ->
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = comment,
                            color = BodyText,
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                }
                if (review.isOwnedByCurrentUser) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        CompactReviewAction(
                            label = "Edit",
                            color = SoloBlue,
                            onClick = onEdit
                        )
                        CompactReviewAction(
                            label = "Delete",
                            color = DPAtriumRed,
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared 5-star row. Read-only when [onRatingChange] is null (used inline in a review card), or
 * interactive when a callback is provided (used in the review form).
 */
@Composable
internal fun StarRow(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 18.dp,
    filledTint: Color = DetailAmber,
    emptyTint: Color = HeaderMuted,
    spacing: Dp = 1.dp,
    onRatingChange: ((Int) -> Unit)? = null,
) {
    val base = if (onRatingChange == null) {
        modifier.clearAndSetSemantics { contentDescription = "$rating out of 5 stars" }
    } else {
        modifier
    }
    Row(
        modifier = base,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(5) { index ->
            val filled = index < rating
            val iconModifier = if (onRatingChange != null) {
                Modifier
                    .size(starSize)
                    .clickable(role = Role.Button) { onRatingChange(index + 1) }
            } else {
                Modifier.size(starSize)
            }
            Icon(
                imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                contentDescription = if (onRatingChange != null) "Rate ${index + 1}" else null,
                tint = if (filled) filledTint else emptyTint,
                modifier = iconModifier
            )
        }
    }
}

/**
 * Small top-of-screen header used by the review and submit-spot forms. Balances a back button on
 * the left with an equal-sized spacer on the right so the centred title stays optically centred.
 */
@Composable
internal fun ScreenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(HomeBackground, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Ink,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = subtitle, color = HeaderMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun CompactReviewAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpotActionButtons(
    accent: Color,
    sessionActiveHere: Boolean,
    sessionActiveElsewhere: Boolean = false,
    activeCheckInSpotName: String? = null,
    checkInLabel: String = "Start Session",
    onCheckIn: () -> Unit,
    onEndSession: () -> Unit,
    onReview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = DetailCardBorder)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            sessionActiveHere -> Button(
                onClick = onEndSession,
                modifier = Modifier.weight(1.15f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DPAtriumRed)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("End Session", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
            sessionActiveElsewhere -> Button(
                onClick = onEndSession,
                modifier = Modifier.weight(1.15f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DPAtriumRed.copy(alpha = 0.85f))
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (activeCheckInSpotName != null) "End session at $activeCheckInSpotName" else "End current session",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            else -> Button(
                onClick = onCheckIn,
                modifier = Modifier.weight(1.15f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(checkInLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        OutlinedButton(
            onClick = onReview,
            modifier = Modifier.weight(0.85f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, SoloBlue)
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = DetailAmber, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Review", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BookRoomBanner(url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
            .background(DetailBlueSoft, RoundedCornerShape(18.dp))
            .border(1.dp, DetailBlueBorder, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button) {
                val parsed = runCatching { Uri.parse(url) }.getOrNull()
                    ?.takeIf { it.scheme?.startsWith("http") == true }
                if (parsed == null) {
                    Toast.makeText(context, "This booking link isn't valid.", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, parsed)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No browser available to open the booking page.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Book this room",
                color = SoloBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Reserve via the official booking page",
                color = HeaderMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Open booking page",
            tint = SoloBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}

private val StudySpotDetail.isLive: Boolean get() = peopleHere > 0

private fun List<FriendProfile>.friendStatusLabel(): String = when (size) {
    0 -> ""
    1 -> "${first().firstName} is studying here"
    2 -> "${this[0].firstName} and ${this[1].firstName} are studying here"
    else -> "${this[0].firstName}, ${this[1].firstName}, and ${size - 2} more are studying here"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
