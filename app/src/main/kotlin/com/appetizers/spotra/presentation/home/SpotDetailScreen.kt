package com.appetizers.spotra.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.domain.repository.ReviewRepository
import com.appetizers.spotra.presentation.toUserMessage
import java.util.Locale
import kotlinx.coroutines.launch

private enum class ReviewFilter { Friends, All }

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
    onReview: () -> Unit,
    onEditReview: (Review) -> Unit = {},
    activeCheckInSpotId: String? = null,
    onEndSession: () -> Unit = {}
) {
    val sessionActiveHere = activeCheckInSpotId == spotId

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

    LaunchedEffect(spotId) {
        error = null
        reviewLoadError = null
        runCatching { homeRepository.spotDetail(spotId) }
            .onSuccess { spot = it }
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

    val displayedReviews = when (reviewFilter) {
        ReviewFilter.Friends -> reviews.filter { it.reviewerId == selfId || it.reviewerId in acceptedFriendIds }
        ReviewFilter.All -> reviews
    }

    BackHandler(onBack = onBack)

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
            .background(Color.White)
    ) {
        SpotDetailHeader(
            spot = detail,
            friendsStudying = friendsAtSpot,
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SpotStatTiles(spot = detail) }
            item { SpotAmenitiesSection(amenities = detail.amenities) }
            detail.bookingUrl?.let { url ->
                item { BookRoomBanner(url = url) }
            }
            if (reviews.isNotEmpty() || reviewLoadError != null) {
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
            item {
                SpotActionButtons(
                    accent = accent,
                    sessionActiveHere = sessionActiveHere,
                    checkInLabel = checkInLabel,
                    onCheckIn = { onCheckIn(detail.toSummary()) },
                    onEndSession = onEndSession,
                    onReview = onReview
                )
            }
        }
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
private fun SpotDetailHeader(
    spot: StudySpotDetail,
    friendsStudying: List<FriendProfile>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CheckInHeader)
            .statusBarsPadding()
            .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(HeaderButton, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.size(44.dp))
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = spot.name,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
    }
}

@Composable
private fun SpotStatTiles(spot: StudySpotDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(emoji = "🤫", value = spot.noiseLevel ?: "No data", label = "NOISE", modifier = Modifier.weight(1f))
            StatTile(emoji = "☀️", value = spot.lighting ?: "No data", label = "LIGHTING", modifier = Modifier.weight(1f))
            StatTile(
                emoji = "📶",
                value = spot.wifiQuality ?: "No data",
                label = "WI-FI",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                value = spot.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "New",
                label = "RATING",
                valueColor = SoloBlue,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = spot.occupancyPercent?.let { "$it%" } ?: "No data",
                label = "% FULL",
                valueColor = if ((spot.occupancyPercent ?: 0) > 70) DPAtriumRed else GroupGreen,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    valueColor: Color = Ink
) {
    Column(
        modifier = modifier
            .background(HomeBackground, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = if (emoji != null) 14.sp else 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, color = HeaderMuted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun SpotAmenitiesSection(amenities: List<String>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(text = "Amenities", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(12.dp))
        amenities.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { amenity ->
                    Text(
                        text = amenity,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.5.dp, DividerLine, RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        color = BodyText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
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
            .padding(start = 20.dp, top = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recent reviews", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (showFilter) {
                ReviewFilter.entries.forEach { filter ->
                    val selected = filter == reviewFilter
                    Text(
                        text = filter.name,
                        modifier = Modifier
                            .background(
                                if (selected) SoloBlue else HomeBackground,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (selected) Color.White else HeaderMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (filter == ReviewFilter.Friends) Spacer(Modifier.width(6.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            loadError != null -> Text(
                text = loadError,
                color = DPAtriumRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            reviews.isEmpty() -> Text(
                text = if (reviewFilter == ReviewFilter.Friends) "No friend reviews yet." else "No reviews yet.",
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            else -> reviews.forEachIndexed { index, review ->
                ReviewRow(
                    review = review,
                    onEdit = { onEditReview(review) },
                    onDelete = { onDeleteReview(review) },
                )
                if (index < reviews.lastIndex) {
                    Spacer(Modifier.height(1.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerLine))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(
    review: Review,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(avatarColorFor(review.reviewerId), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = review.reviewerInitials,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = review.reviewerName, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            review.comment?.let { comment ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = comment,
                    color = BodyText,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (review.isOwnedByCurrentUser) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Edit",
                        color = SoloBlue,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onEdit),
                    )
                    Text(
                        text = "Delete",
                        color = DPAtriumRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onDelete),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Row {
            repeat(5) { i ->
                Icon(
                    imageVector = if (i < review.rating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = null,
                    tint = StarGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotActionButtons(
    accent: Color,
    sessionActiveHere: Boolean,
    checkInLabel: String = "Start Session",
    onCheckIn: () -> Unit,
    onEndSession: () -> Unit,
    onReview: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 24.dp, end = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sessionActiveHere) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(DPAtriumRed, RoundedCornerShape(16.dp))
                    .clickable(onClick = onEndSession),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("End Session", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(accent, RoundedCornerShape(16.dp))
                    .clickable(onClick = onCheckIn),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(checkInLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .border(1.5.dp, DividerLine, RoundedCornerShape(16.dp))
                .clickable(onClick = onReview),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.Star, null, tint = StarGold, modifier = Modifier.size(18.dp))
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
            .background(CardBackground, RoundedCornerShape(16.dp))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
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
