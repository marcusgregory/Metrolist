package com.metrolist.music.wear.presentation.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.data.model.WearArtist

/**
 * A card component for displaying an artist in the search results.
 * Uses circular thumbnail to match YouTube Music design.
 */
@Composable
fun ArtistCard(
    artist: WearArtist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(artist.thumbnailUrl)
            .size(64)
            .crossfade(false)
            .memoryCacheKey(artist.id)
            .build()
    )

    Button(
        onClick = onClick,
        icon = {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        label = {
            Text(
                text = artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = "Artist",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier.fillMaxWidth()
    )
}
