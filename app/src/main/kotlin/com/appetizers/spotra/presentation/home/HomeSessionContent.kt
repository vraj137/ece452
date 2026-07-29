package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.domain.model.CheckInSession
import com.appetizers.spotra.domain.model.CheckedInStudent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostCheckoutReviewSheet(
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
internal fun LiveCheckInScreen(
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
    var selectedBuddy by remember { mutableStateOf<CheckedInStudent?>(null) }

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
internal fun ActiveSessionBar(
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

private fun Int.asSessionTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
