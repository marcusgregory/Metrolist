package com.metrolist.music.wear.presentation.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.presentation.components.EqualizerIcon

/**
 * Data class for now playing information.
 */
data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
    val isBuffering: Boolean = false
) {
    /** Show animation when playing or buffering (intent to play) */
    val shouldAnimate: Boolean get() = isPlaying || isBuffering
}

/**
 * A chip that displays the currently playing song with thumbnail, title, artist,
 * and an animated equalizer indicator when playing.
 *
 * Design pattern used by Spotify, YouTube Music, and Deezer on WearOS.
 */
@Composable
fun NowPlayingChip(
    info: NowPlayingInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(info.artworkUri)
            .size(64)
            .crossfade(true)
            .build()
    )

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(),
        icon = {
            // Album artwork - circular like WearOS standard
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
                text = info.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated equalizer when playing or buffering
                if (info.shouldAnimate) {
                    EqualizerIcon(
                        modifier = Modifier.size(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        animating = info.isPlaying // Animate bars only when truly playing, static when buffering
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = info.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
