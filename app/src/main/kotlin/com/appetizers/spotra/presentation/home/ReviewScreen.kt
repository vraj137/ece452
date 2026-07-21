package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.ReviewDraft
import com.appetizers.spotra.domain.repository.ReviewRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val NOISE_OPTIONS = listOf("Silent", "Low", "Moderate", "Lively")
private val LIGHTING_OPTIONS = listOf("Poor", "Good", "Bright", "Natural")
private val WIFI_OPTIONS = listOf("Poor", "OK", "Good", "Fast")
private val OCCUPANCY_OPTIONS = listOf("Empty", "Some", "Busy", "Packed")

private fun occupancyToPercent(index: Int?): Int? = when (index) {
    0 -> 10
    1 -> 40
    2 -> 70
    3 -> 95
    else -> null
}

internal fun noiseLabel(index: Int?): String? = index?.let { NOISE_OPTIONS.getOrNull(it) }
internal fun lightingLabel(index: Int?): String? = index?.let { LIGHTING_OPTIONS.getOrNull(it) }

@Composable
internal fun ReviewScreen(
    spotName: String,
    spotSlug: String,
    reviewRepository: ReviewRepository,
    onBack: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var noiseIndex by remember { mutableStateOf<Int?>(null) }
    var lightingIndex by remember { mutableStateOf<Int?>(null) }
    var wifiIndex by remember { mutableStateOf<Int?>(null) }
    var occupancyIndex by remember { mutableStateOf<Int?>(null) }
    var comment by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (submitted) {
        ReviewSuccessScreen(onBack = onBack)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        ReviewHeader(spotName = spotName, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            StarRatingRow(rating = rating, onRatingChange = { rating = it })

            LabelSlider(label = "Noise level", options = NOISE_OPTIONS, selectedIndex = noiseIndex, onSelect = { noiseIndex = it })
            LabelSlider(label = "Lighting", options = LIGHTING_OPTIONS, selectedIndex = lightingIndex, onSelect = { lightingIndex = it })
            LabelSlider(label = "WiFi quality", options = WIFI_OPTIONS, selectedIndex = wifiIndex, onSelect = { wifiIndex = it })
            LabelSlider(label = "How busy was it?", options = OCCUPANCY_OPTIONS, selectedIndex = occupancyIndex, onSelect = { occupancyIndex = it })

            ReviewCommentField(value = comment, onValueChange = { comment = it })

            AnonymousToggle(checked = anonymous, onToggle = { anonymous = !anonymous })

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color(0xFFD83D3C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            val canSubmit = rating > 0 && !isLoading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (rating > 0) SoloBlue else SoloBlue.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = canSubmit) {
                        scope.launch {
                            isLoading = true
                            error = null
                            runCatching {
                                reviewRepository.submit(
                                    ReviewDraft(
                                        spotSlug = spotSlug,
                                        rating = rating,
                                        noiseLevel = noiseLabel(noiseIndex),
                                        lighting = lightingLabel(lightingIndex),
                                        wifiQuality = wifiIndex?.let { WIFI_OPTIONS.getOrNull(it) },
                                        occupancyPercent = occupancyToPercent(occupancyIndex),
                                        comment = comment.trim().ifBlank { null },
                                        anonymous = anonymous
                                    )
                                )
                                submitted = true
                            }.onFailure {
                                error = friendlyError(it.message)
                            }
                            isLoading = false
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoading) "Posting..." else "Post review",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun friendlyError(raw: String?): String {
    val message = raw.orEmpty()
    val blockedByGate = message.contains("row-level security", ignoreCase = true) ||
        message.contains("policy", ignoreCase = true) ||
        message.contains("403")
    return if (blockedByGate) {
        "You can only review a spot after checking in here. Check in first, then post your review."
    } else {
        message.ifBlank { "Something went wrong. Try again." }
    }
}

@Composable
private fun ReviewHeader(spotName: String, onBack: () -> Unit) {
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
                .clickable(onClick = onBack),
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
            Text(text = "Write a review", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = spotName, color = HeaderMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun StarRatingRow(rating: Int, onRatingChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (rating > 0) ratingLabel(rating) else "Tap to rate",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { index ->
                val filled = index < rating
                Icon(
                    imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Rate ${index + 1}",
                    tint = if (filled) StarGold else DividerLine,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onRatingChange(index + 1) }
                )
            }
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    1 -> "Poor"
    2 -> "Fair"
    3 -> "Good"
    4 -> "Great"
    else -> "Excellent"
}

@Composable
internal fun LabelSlider(
    label: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    accentColor: Color = SoloBlue,
) {
    val hasValue = selectedIndex != null
    var sliderPosition by remember(selectedIndex) {
        mutableFloatStateOf(selectedIndex?.toFloat() ?: 0f)
    }
    val displayLabel = selectedIndex?.let { options.getOrNull(it) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = displayLabel ?: "optional",
                color = if (hasValue) accentColor else HeaderMuted,
                fontSize = 14.sp,
                fontWeight = if (hasValue) FontWeight.ExtraBold else FontWeight.Normal,
            )
        }
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                onSelect(newValue.roundToInt())
            },
            valueRange = 0f..(options.size - 1).toFloat(),
            steps = options.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = if (hasValue) accentColor else HeaderMuted,
                activeTrackColor = if (hasValue) accentColor else HeaderMuted,
                inactiveTrackColor = DividerLine,
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { option ->
                Text(option, color = HeaderMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ReviewCommentField(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = "Comments (optional)", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.5.dp, DividerLine, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "Anything else students should know?",
                    color = HeaderMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = 3,
                textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Normal),
                cursorBrush = SolidColor(SoloBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AnonymousToggle(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (checked) SoloBlue else Color.White,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    1.5.dp,
                    if (checked) SoloBlue else DividerLine,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = "Post anonymously", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Your name won't be shown to other students",
                color = HeaderMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReviewSuccessScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(QuietPill, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GroupGreen, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Review posted!", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Thanks for helping other students. Your report updates this spot's live status for everyone.",
            color = HeaderMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SoloBlue, RoundedCornerShape(16.dp))
                .clickable(onClick = onBack),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Done", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
