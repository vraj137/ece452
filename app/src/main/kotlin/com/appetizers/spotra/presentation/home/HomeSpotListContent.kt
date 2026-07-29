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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.HomeRepository
import com.appetizers.spotra.presentation.toUserMessage
import androidx.compose.material3.Button

@Composable
internal fun BuildingSpacesScreen(
    parentSpot: StudySpotSummary,
    homeRepository: HomeRepository,
    accent: Color,
    onBack: () -> Unit,
    onSpaceSelected: (StudySpotDetail) -> Unit
) {
    var spaces by remember(parentSpot.id) { mutableStateOf<List<StudySpotDetail>>(emptyList()) }
    var isLoading by remember(parentSpot.id) { mutableStateOf(true) }
    var error by remember(parentSpot.id) { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

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
                    .size(48.dp)
                    .background(HomeBackground, RoundedCornerShape(14.dp))
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
internal fun StudySpotCard(
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

internal fun badgePillColors(badge: String): Pair<Color, Color> = when (badge.lowercase()) {
    "silent", "quiet" -> QuietPill to QuietText
    "moderate" -> ModerateFitBackground to ModerateFitText
    "busy", "lively" -> Color(0xFFFFECEC) to Color(0xFFB91C1C)
    else -> SwitcherTrack to BodyText
}
