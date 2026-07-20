package com.appetizers.spotra.presentation.home

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.appetizers.spotra.BuildConfig
import com.appetizers.spotra.domain.model.SpotSubmission
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.SpotSubmissionRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.viewannotation.annotationAnchor
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import com.mapbox.maps.ViewAnnotationAnchor
import kotlinx.coroutines.launch

private const val UW_LAT = 43.4720
private const val UW_LNG = -80.5430

@Composable
internal fun SubmitSpotScreen(
    authRepository: AuthRepository,
    spotSubmissionRepository: SpotSubmissionRepository,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var building by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var pinLat by remember { mutableDoubleStateOf(UW_LAT) }
    var pinLng by remember { mutableDoubleStateOf(UW_LNG) }
    var pinPlaced by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showFullMap by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler {
        if (showFullMap) {
            showFullMap = false
        } else {
            onBack()
        }
    }

    if (submitted) {
        SubmitSuccessScreen(onBack = onBack)
        return
    }

    if (showFullMap) {
        FullScreenMapPicker(
            pinLat = pinLat,
            pinLng = pinLng,
            pinPlaced = pinPlaced,
            onPinPlaced = { lat, lng ->
                pinLat = lat
                pinLng = lng
                pinPlaced = true
            },
            onConfirm = { showFullMap = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding()
    ) {
        SubmitSpotHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SubmitField(
                label = "Spot name",
                value = name,
                onValueChange = { name = it },
                placeholder = "e.g. DC Library 4th Floor"
            )

            SubmitField(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                placeholder = "Quiet area, good WiFi, plenty of outlets...",
                minLines = 3
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    SubmitField(
                        label = "Building",
                        value = building,
                        onValueChange = { building = it },
                        placeholder = "e.g. DC"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SubmitField(
                        label = "Floor",
                        value = floor,
                        onValueChange = { floor = it },
                        placeholder = "e.g. 3rd"
                    )
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pin location",
                        color = Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.weight(1f))
                    if (pinPlaced) {
                        Text(
                            text = "%.4f, %.4f".format(pinLat, pinLng),
                            color = SoloBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap anywhere on the map to place your pin",
                    color = HeaderMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                SpotLocationPicker(
                    pinLat = pinLat,
                    pinLng = pinLng,
                    pinPlaced = pinPlaced,
                    onPinPlaced = { lat, lng ->
                        pinLat = lat
                        pinLng = lng
                        pinPlaced = true
                    },
                    onExpand = { showFullMap = true }
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color(0xFFD83D3C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (name.isNotBlank()) SoloBlue else SoloBlue.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = name.isNotBlank() && !isLoading) {
                        scope.launch {
                            isLoading = true
                            error = null
                            runCatching {
                                val user = authRepository.currentUser()
                                spotSubmissionRepository.submitSpot(
                                    SpotSubmission(
                                        name = name.trim(),
                                        description = description.trim(),
                                        latitude = pinLat,
                                        longitude = pinLng,
                                        building = building.trim(),
                                        floor = floor.trim(),
                                        submittedByEmail = user?.email ?: "",
                                        submittedByUserId = user?.id
                                    )
                                )
                                submitted = true
                            }.onFailure {
                                error = it.message ?: "Something went wrong. Try again."
                            }
                            isLoading = false
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLoading) "Submitting..." else "Submit for review",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SubmitSpotHeader(onBack: () -> Unit) {
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
            Text(text = "Suggest a spot", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "Our team will review your submission", color = HeaderMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun SubmitField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1
) {
    Column {
        Text(text = label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.5.dp, DividerLine, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, color = HeaderMuted, fontSize = 16.sp, fontWeight = FontWeight.Normal)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = minLines,
                textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Normal),
                cursorBrush = SolidColor(SoloBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SpotLocationPicker(
    pinLat: Double,
    pinLng: Double,
    pinPlaced: Boolean,
    onPinPlaced: (Double, Double) -> Unit,
    onExpand: () -> Unit
) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(UW_LNG, UW_LAT))
            zoom(15.0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MapLoadingBackground, RoundedCornerShape(16.dp))
            .border(1.5.dp, DividerLine, RoundedCornerShape(16.dp))
    ) {
        if (BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = SoloBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Map unavailable in debug mode", color = HeaderMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (pinPlaced) {
                        Spacer(Modifier.height(4.dp))
                        Text("Using UW campus center", color = SoloBlue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = { MapStyle(style = Style.LIGHT) },
                scaleBar = {},
                logo = {},
                attribution = {},
                onMapClickListener = { point ->
                    onPinPlaced(point.latitude(), point.longitude())
                    true
                }
            ) {
                if (pinPlaced) {
                    val options = remember(pinLat, pinLng) {
                        viewAnnotationOptions {
                            geometry(Point.fromLngLat(pinLng, pinLat))
                            annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                            allowOverlap(true)
                        }
                    }
                    ViewAnnotation(options = options) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = SoloBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        if (!pinPlaced) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(Ink.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Tap to drop a pin", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(34.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                .clickable(onClick = onExpand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = "Expand map",
                tint = Ink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FullScreenMapPicker(
    pinLat: Double,
    pinLng: Double,
    pinPlaced: Boolean,
    onPinPlaced: (Double, Double) -> Unit,
    onConfirm: () -> Unit
) {
    val mapUnavailable = BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank()
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(if (pinPlaced) pinLng else UW_LNG, if (pinPlaced) pinLat else UW_LAT))
            zoom(16.0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MapLoadingBackground)) {
        if (mapUnavailable) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = SoloBlue,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Map unavailable in debug mode",
                        color = HeaderMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (pinPlaced) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Using selected location",
                            color = SoloBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = { MapStyle(style = Style.LIGHT) },
                scaleBar = {},
                logo = {},
                attribution = {},
                onMapClickListener = { point ->
                    onPinPlaced(point.latitude(), point.longitude())
                    true
                }
            ) {
                if (pinPlaced) {
                    val options = remember(pinLat, pinLng) {
                        viewAnnotationOptions {
                            geometry(Point.fromLngLat(pinLng, pinLat))
                            annotationAnchor { anchor(ViewAnnotationAnchor.BOTTOM) }
                            allowOverlap(true)
                        }
                    }
                    ViewAnnotation(options = options) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = SoloBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        if (!pinPlaced && !mapUnavailable) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Ink.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Tap anywhere to drop your pin", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pinPlaced) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(SoloBlue, RoundedCornerShape(16.dp))
                        .clickable(onClick = onConfirm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Confirm location", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .clickable(onClick = onConfirm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Close", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SubmitSuccessScreen(onBack: () -> Unit) {
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
                .background(GroupBestFitBackground, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GroupGreen, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Spot submitted!", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Thanks for helping grow Spotra. Our team will review your suggestion and add it to the map if it meets our guidelines.",
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
            Text("Back to explore", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
