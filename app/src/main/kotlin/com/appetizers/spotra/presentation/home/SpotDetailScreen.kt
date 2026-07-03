package com.appetizers.spotra.presentation.home

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
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.data.mock.MockData
import com.appetizers.spotra.data.mock.MockSpot
import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.Review
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.FriendRepository
import com.appetizers.spotra.domain.repository.ReviewRepository

private enum class ReviewFilter { Friends, All }

@Composable
internal fun SpotDetailScreen(
    spotId: String,
    accent: Color,
    onBack: () -> Unit,
    onCheckIn: (StudySpotSummary) -> Unit,
    reviewRepository: ReviewRepository,
    friendRepository: FriendRepository? = null,
    onReview: () -> Unit,
    activeCheckInSpotId: String? = null,
    onEndSession: () -> Unit = {}
) {
    val spot = MockData.spotById(spotId)

    if (spot == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val sessionActiveHere = activeCheckInSpotId == spotId

    var reviews by remember(spotId) { mutableStateOf<List<Review>>(emptyList()) }
    var friendsAtSpot by remember(spotId) { mutableStateOf<List<FriendProfile>>(emptyList()) }
    var acceptedFriendIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selfId by remember { mutableStateOf<String?>(null) }
    var reviewFilter by remember { mutableStateOf(ReviewFilter.All) }

    LaunchedEffect(spotId) {
        reviews = runCatching { reviewRepository.reviewsFor(spotId) }.getOrDefault(emptyList())
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        SpotDetailHeader(
            spot = spot,
            friendsStudying = friendsAtSpot.map { it.id to it.initials },
            onBack = onBack,
            onCheckIn = { onCheckIn(spot.toSummary()) }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SpotStatTiles(spot = spot) }
            item { SpotAmenitiesSection(amenities = spot.amenities) }
            if (reviews.isNotEmpty()) {
                item {
                    SpotReviewsSection(
                        reviews = displayedReviews,
                        reviewFilter = reviewFilter,
                        onFilterChange = { reviewFilter = it },
                        showFilter = friendRepository != null
                    )
                }
            }
            item {
                SpotActionButtons(
                    accent = accent,
                    sessionActiveHere = sessionActiveHere,
                    onCheckIn = { onCheckIn(spot.toSummary()) },
                    onEndSession = onEndSession,
                    onReview = onReview
                )
            }
        }
    }
}

@Composable
private fun SpotDetailHeader(
    spot: MockSpot,
    friendsStudying: List<Pair<String, String>>,
    onBack: () -> Unit,
    onCheckIn: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(HeaderButton, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
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

        Text(
            text = "${spot.building} · ${spot.distanceMeters}m away",
            color = HeaderSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )

        if (spot.isLive) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(CheckedInDot, CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Live · ${spot.peopleHere} people here now",
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
                    friendsStudying.forEach { (id, initials) ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(avatarColorFor(id), CircleShape)
                                .border(2.dp, CheckInHeader, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Akshat and ${MockData.user.firstName} are studying here",
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
private fun SpotStatTiles(spot: MockSpot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(emoji = "🤫", value = spot.noiseLevel, label = "NOISE", modifier = Modifier.weight(1f))
        StatTile(emoji = "☀️", value = spot.lighting, label = "LIGHTING", modifier = Modifier.weight(1f))
        StatTile(
            value = String.format("%.1f", spot.rating),
            label = "RATING",
            valueColor = SoloBlue,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = "${spot.fullPercent}%",
            label = "FULL",
            valueColor = if (spot.fullPercent > 70) DPAtriumRed else GroupGreen,
            modifier = Modifier.weight(1f)
        )
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
    showFilter: Boolean = false
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
        if (reviews.isEmpty()) {
            Text(
                text = if (reviewFilter == ReviewFilter.Friends) "No friend reviews yet." else "No reviews yet.",
                color = HeaderMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            reviews.forEachIndexed { index, review ->
                ReviewRow(review = review)
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
private fun ReviewRow(review: Review) {
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
                Text("Start Session", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
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

// Needed by SpotDetailHeader but not in scope from HomeColors since it uses
// a field from MockSpot rather than a raw Boolean.
private val MockSpot.isLive: Boolean get() = peopleHere > 0
