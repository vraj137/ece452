package com.appetizers.spotra.presentation.home

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appetizers.spotra.BuildConfig
import com.appetizers.spotra.domain.model.StudySpotSummary
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

@Composable
internal fun CampusMap(
    spots: List<StudySpotSummary>,
    selectedSpotId: String?,
    onSpotSelected: (String) -> Unit,
    accent: Color,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val located = remember(spots) { spots.filter { it.latitude != null && it.longitude != null } }
    val displayState = mapDisplayState(
        tokenBlank = BuildConfig.MAPBOX_PUBLIC_TOKEN.isBlank(),
        locatedCount = located.size
    )

    if (displayState == MapDisplayState.PLACEHOLDER) {
        CampusMapPlaceholder(modifier = modifier)
        return
    }


    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(MapConfig.CAMPUS_LNG, MapConfig.CAMPUS_LAT))
            zoom(MapConfig.DEFAULT_ZOOM)
        }
    }

    val zoomBy: (Double) -> Unit = { delta ->
        val cameraState = mapViewportState.cameraState
        val nextZoom = MapConfig.coerceZoom((cameraState?.zoom ?: MapConfig.DEFAULT_ZOOM) + delta)
        mapViewportState.easeTo(
            cameraOptions {
                center(cameraState?.center ?: Point.fromLngLat(MapConfig.CAMPUS_LNG, MapConfig.CAMPUS_LAT))
                zoom(nextZoom)
                bearing(cameraState?.bearing ?: 0.0)
                pitch(cameraState?.pitch ?: 0.0)
            },
            MapAnimationOptions.mapAnimationOptions { duration(180) }
        )
    }

    Box(modifier = modifier.background(MapLoadingBackground)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = Style.LIGHT) },
            scaleBar = {},
            logo = {},
            attribution = {}
        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    enabled = true
                    pulsingEnabled = true
                }
            }
            located.forEach { spot ->
                key(spot.id) {
                    val options = remember(spot.id) {
                        viewAnnotationOptions {
                            geometry(Point.fromLngLat(spot.longitude!!, spot.latitude!!))
                            annotationAnchor {
                                anchor(ViewAnnotationAnchor.BOTTOM)
                            }
                            allowOverlap(true)
                        }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    ViewAnnotation(options = options) {
                        MapPin(
                            label = spot.name,
                            color = occupancyPinColor(spot),
                            selected = spot.id == selectedSpotId,
                            sharedPeopleCount = spot.friendsHere,
                            contentDescription = buildString {
                                append("${spot.name}, occupancy ${spot.badge}")
                                if (spot.friendsHere > 0) {
                                    append(", ${spot.friendsHere} people sharing an active check-in here")
                                }
                            },
                            modifier = Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSpotSelected(spot.id) }
                        )
                    }
                }
            }

        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MapCircleButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Zoom in",
                onClick = { zoomBy(1.0) }
            )
            MapCircleButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Zoom out",
                onClick = { zoomBy(-1.0) }
            )
            MapCircleButton(
                icon = Icons.Rounded.Refresh,
                contentDescription = "Refresh spot occupancy",
                loading = isRefreshing,
                onClick = onRefresh
            )
        }

        if (displayState == MapDisplayState.EMPTY) {
            MapEmptyOverlay(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MapEmptyOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Map,
            contentDescription = null,
            tint = HeaderMuted,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "No study spots to show",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tap refresh to try again",
            color = BodyText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MapCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(6.dp, CircleShape, clip = false)
            .background(Color.White, CircleShape)
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = SoloBlue
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CampusMapPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().background(MapBackground)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(MapBackground)
            val blockColor = MapBlock.copy(alpha = .88f)
            val blockRadius = 9.dp.toPx()
            val blockWidth = size.width * .24f
            val blockHeight = size.height * .20f
            val blockPositions = listOf(
                Offset(size.width * .05f, size.height * .10f),
                Offset(size.width * .31f, size.height * .13f),
                Offset(size.width * .63f, size.height * .10f),
                Offset(size.width * .05f, size.height * .47f),
                Offset(size.width * .31f, size.height * .49f),
                Offset(size.width * .63f, size.height * .49f)
            )
            blockPositions.forEach { topLeft ->
                drawRoundRect(color = blockColor, topLeft = topLeft, size = Size(blockWidth, blockHeight), cornerRadius = CornerRadius(blockRadius, blockRadius))
            }
            val streetWidth = 13.dp.toPx()
            listOf(.26f, .58f, .91f).forEach { x ->
                drawLine(Color.White, Offset(size.width * x, 0f), Offset(size.width * x, size.height), streetWidth, StrokeCap.Round)
            }
            listOf(.31f, .59f, .87f).forEach { y ->
                drawLine(Color.White, Offset(0f, size.height * y), Offset(size.width, size.height * y), streetWidth, StrokeCap.Round)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Map, contentDescription = null, tint = HeaderMuted, modifier = Modifier.size(28.dp))
            Text("Map unavailable", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Live places are listed below", color = BodyText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MapPin(
    label: String,
    color: Color,
    selected: Boolean,
    sharedPeopleCount: Int,
    modifier: Modifier = Modifier,
    contentDescription: String = label
) {
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "pin-label-alpha"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pin-dot-scale"
    )

    val pillShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            modifier = Modifier
                .graphicsLayer { alpha = labelAlpha }
                .shadow(6.dp, pillShape, clip = false)
                .background(color, pillShape)
                .border(2.dp, Color.White, pillShape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        if (sharedPeopleCount > 0) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .shadow(4.dp, pillShape, clip = false)
                    .background(SoloBlue, pillShape)
                    .border(1.dp, Color.White, pillShape)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = sharedPeopleCount.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                .size(12.dp)
                .shadow(3.dp, CircleShape, clip = false)
                .background(Color.White, CircleShape)
                .padding(2.5.dp)
                .background(color, CircleShape)
        )
    }
}

private fun occupancyPinColor(spot: StudySpotSummary): Color =
    spot.occupancyPercent?.let(::occupancyPinColorFromPercent) ?: HeaderMuted

private fun occupancyPinColorFromPercent(percent: Int): Color = when {
    percent >= 75 -> DPAtriumRed
    percent >= 50 -> SLCOrange
    else -> LibraryGreen
}
