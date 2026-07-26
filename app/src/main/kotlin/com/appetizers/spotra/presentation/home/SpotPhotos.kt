package com.appetizers.spotra.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.appetizers.spotra.domain.model.SpotPhoto

/**
 * Curated photos of a study spot.
 *
 * Photos are supplementary: everything they convey about a spot (how busy, how bright, how loud)
 * is also stated in text elsewhere on the screen, so a screen-reader user loses nothing by
 * skipping them. Renders nothing at all when a spot has no photos rather than leaving an empty
 * frame behind.
 */
@Composable
internal fun SpotPhotoGallery(
    photos: List<SpotPhoto>,
    spotName: String,
    modifier: Modifier = Modifier,
) {
    if (photos.isEmpty()) return

    if (photos.size == 1) {
        SpotPhotoImage(
            photo = photos.first(),
            spotName = spotName,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        return
    }

    LazyRow(
        modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(photos) { index, photo ->
            SpotPhotoImage(
                photo = photo,
                spotName = spotName,
                position = index + 1,
                total = photos.size,
                modifier = Modifier
                    .width(268.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(18.dp))
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
