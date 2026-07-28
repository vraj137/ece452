package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.appetizers.spotra.domain.model.SpotPhoto

/**
 * A spot's photos as a swipeable backdrop for the detail header.
 *
 * The scrim is not decorative: the spot name renders as 28sp white text on top of whatever the
 * photo happens to be, and an unscrimmed shot of a bright window would swallow it. The gradient
 * leaves the top of the image legible and lands on essentially the same navy the header used
 * before photos existed.
 *
 * Each page carries its own content description including position, so a screen-reader user gets
 * what the dots convey visually.
 */
@Composable
internal fun SpotPhotoBackdrop(
    photos: List<SpotPhoto>,
    spotName: String,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(CheckInHeader)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            SpotPhotoImage(
                photo = photos[page],
                spotName = spotName,
                position = page + 1,
                total = photos.size,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to CheckInHeader.copy(alpha = 0.55f),
                        1f to CheckInHeader.copy(alpha = 0.92f)
                    )
                )
        )
    }
}

/**
 * Page position for the photo backdrop. Purely visual, and silent to a screen reader by
 * construction: these are bare unlabelled boxes, and the pager page itself already announces
 * "(2 of 3)", so repeating it here would just be noise.
 */
@Composable
internal fun SpotPhotoDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = if (index == selectedIndex) Color.White else Color.White.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/** Small cover image for a spot in a list. Falls back to a neutral tile when there is no photo. */
@Composable
internal fun SpotThumbnail(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val shape = RoundedCornerShape(14.dp)
    if (photoUrl == null) {
        SpotPhotoFallback(modifier = modifier.size(size).clip(shape))
        return
    }
    SubcomposeAsyncImage(
        model = photoUrl,
        // The adjacent text already names the spot; announcing the photo too would just repeat it.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { SpotPhotoFallback(Modifier.fillMaxSize()) },
        error = { SpotPhotoFallback(Modifier.fillMaxSize()) },
        modifier = modifier.size(size).clip(shape)
    )
}

@Composable
private fun SpotPhotoImage(
    photo: SpotPhoto,
    spotName: String,
    modifier: Modifier = Modifier,
    position: Int? = null,
    total: Int? = null,
) {
    val description = buildString {
        append(photo.caption?.takeIf { it.isNotBlank() } ?: "Photo of $spotName")
        if (position != null && total != null && total > 1) {
            append(" (")
            append(position)
            append(" of ")
            append(total)
            append(")")
        }
    }
    SubcomposeAsyncImage(
        model = photo.url,
        contentDescription = description,
        contentScale = ContentScale.Crop,
        loading = { SpotPhotoFallback(Modifier.fillMaxSize()) },
        error = { SpotPhotoFallback(Modifier.fillMaxSize()) },
        modifier = modifier.semantics { this.contentDescription = description }
    )
}

/** Shown while loading and when a URL fails, so a dead link never becomes a broken-image glyph. */
@Composable
private fun SpotPhotoFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(SwitcherTrack),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = DividerLine,
            modifier = Modifier.size(24.dp)
        )
    }
}
