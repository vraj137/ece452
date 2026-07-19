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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.rememberLazyListState
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

private enum class LeaderboardCategory(val label: String, val subtitle: String) {
    Trending("Trending", "Top spots this week"),
    Quietest("Quietest", "Lowest noise levels"),
    BestLighting("Best Lighting", "Best-lit study spots"),
    TopRated("Top Rated", "Highest rated by students"),
}

@Composable
internal fun ExploreTabContent(
    accent: Color,
    trendingCounts: Map<String, Int>,
    onSpotSelected: (String) -> Unit,
    onSuggestSpot: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(LeaderboardCategory.Trending) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }

    val rankedSpots = remember(trendingCounts, selectedCategory) {
        when (selectedCategory) {
            LeaderboardCategory.Trending ->
                MockData.spots.sortedByDescending { trendingCounts[it.id] ?: 0 }
            LeaderboardCategory.Quietest ->
                MockData.spots.sortedBy { noiseSortKey(it.noiseLevel) }
            LeaderboardCategory.BestLighting ->
                MockData.spots.sortedBy { lightingSortKey(it.lighting) }
            LeaderboardCategory.TopRated ->
                MockData.spots.sortedByDescending { it.rating }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        ExploreHeader(
            accent = accent,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            onSuggestSpot = onSuggestSpot
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = rankedSpots,
                key = { _, spot -> spot.id }
            ) { index, spot ->
                LeaderboardSpotCard(
                    spot = spot,
                    rank = index + 1,
                    category = selectedCategory,
                    checkIns = trendingCounts[spot.id] ?: 0,
                    accent = accent,
                    onClick = { onSpotSelected(spot.id) }
                )
            }
        }
    }
}

@Composable
private fun ExploreHeader(
    accent: Color,
    selectedCategory: LeaderboardCategory,
    onCategorySelected: (LeaderboardCategory) -> Unit,
    onSuggestSpot: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 22.dp, top = 18.dp, end = 22.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "sp", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "o", color = accent, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "tra", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = selectedCategory.subtitle,
            color = HeaderMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeaderboardCategory.entries.forEach { category ->
                item(key = category.name) {
                    CategoryChip(
                        label = category.label,
                        selected = category == selectedCategory,
                        accent = accent,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedCategory == LeaderboardCategory.Trending) {
                Row(
                    modifier = Modifier
                        .background(SwitcherTrack, RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(GroupGreen, CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "Updated live",
                        color = BodyText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(Modifier.width(0.dp))
            }
            Row(
                modifier = Modifier
                    .background(accent, RoundedCornerShape(22.dp))
                    .clickable(onClick = onSuggestSpot)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ Suggest a spot",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) accent else SwitcherTrack,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else BodyText,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun LeaderboardSpotCard(
    spot: MockSpot,
    rank: Int,
    category: LeaderboardCategory,
    checkIns: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val isTopThree = rank <= 3
    val borderColor = if (isTopThree) accent.copy(alpha = 0.35f) else DividerLine
    val cardBg = if (isTopThree) CardBackground else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(20.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank = rank, accent = accent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spot.name,
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = spot.building,
                    color = HeaderMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            when (category) {
                LeaderboardCategory.Trending -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$checkIns",
                            color = accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "check-ins",
                            color = HeaderMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                LeaderboardCategory.Quietest -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = spot.noiseLevel,
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "noise",
                            color = HeaderMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                LeaderboardCategory.BestLighting -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = spot.lighting,
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "lighting",
                            color = HeaderMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                LeaderboardCategory.TopRated -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", spot.rating),
                            color = accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = NavMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpotAttributePill(label = spot.noiseLevel, category = "Noise")
            SpotAttributePill(label = spot.lighting, category = "Light")
            RatingPill(rating = spot.rating)
            FullnessPill(percent = spot.fullPercent)
        }
    }
}

@Composable
private fun RankBadge(rank: Int, accent: Color) {
    val (bg, textColor) = when (rank) {
        1 -> Color(0xFFFFD700) to Color(0xFF7A5800)
        2 -> Color(0xFFDDE3EE) to Color(0xFF4A5568)
        3 -> Color(0xFFEED8C8) to Color(0xFF7A3F1E)
        else -> SwitcherTrack to HeaderMuted
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SpotAttributePill(label: String, category: String) {
    val (bg, fg) = when (label.lowercase()) {
        "silent", "low" -> QuietPill to QuietText
        "moderate" -> ModerateFitBackground to ModerateFitText
        "lively", "high", "poor" -> Color(0xFFFFECEC) to Color(0xFFB91C1C)
        "good", "bright", "natural" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> SwitcherTrack to BodyText
    }
    Text(
        text = label,
        modifier = Modifier
            .background(bg, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

@Composable
private fun RatingPill(rating: Double) {
    Row(
        modifier = Modifier
            .background(SwitcherTrack, RoundedCornerShape(22.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = StarGold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = String.format("%.1f", rating),
            color = BodyText,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun FullnessPill(percent: Int) {
    val (bg, fg) = when {
        percent >= 80 -> Color(0xFFFFECEC) to Color(0xFFB91C1C)
        percent >= 50 -> ModerateFitBackground to ModerateFitText
        else -> QuietPill to QuietText
    }
    Text(
        text = "$percent% full",
        modifier = Modifier
            .background(bg, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1
    )
}

private fun noiseSortKey(noiseLevel: String): Int = when (noiseLevel.lowercase()) {
    "silent" -> 0
    "low" -> 1
    "moderate" -> 2
    "lively" -> 3
    else -> 4
}

private fun lightingSortKey(lighting: String): Int = when (lighting.lowercase()) {
    "bright" -> 0
    "natural" -> 1
    "good" -> 2
    "poor" -> 3
    else -> 4
}
