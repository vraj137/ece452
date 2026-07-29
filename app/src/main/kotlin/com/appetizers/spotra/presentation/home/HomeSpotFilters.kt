package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.StudyMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet

private val NOISE_FILTER_OPTIONS = listOf("Silent", "Low", "Moderate", "Lively")
private val LIGHTING_FILTER_OPTIONS = listOf("Poor", "Good", "Bright", "Natural")
private val WIFI_FILTER_OPTIONS = listOf("Poor", "OK", "Good", "Fast")
private val AMENITY_OPTIONS = listOf("Wi-Fi", "Outlets", "Whiteboard", "Accessible")
private val OCCUPANCY_OPTIONS = listOf(50 to "< 50% full", 75 to "< 75% full")

@Composable
internal fun FilterControl(activeFilterCount: Int, accent: Color, onClick: () -> Unit) {
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
internal fun SpotFiltersSheet(
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

internal fun noiseIcon(label: String): ImageVector = when (label.lowercase()) {
    "silent", "quiet" -> Icons.AutoMirrored.Rounded.VolumeOff
    else -> Icons.AutoMirrored.Rounded.VolumeDown
}
