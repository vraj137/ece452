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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.BuildConfig
import com.appetizers.spotra.domain.model.StudyMode
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.domain.model.StudySpotSummary
import com.appetizers.spotra.domain.repository.HomeRepository
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MapTabContent(
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
        SpotCard(
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
