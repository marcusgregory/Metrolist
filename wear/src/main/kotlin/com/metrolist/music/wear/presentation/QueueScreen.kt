package com.metrolist.music.wear.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.player.QueueItemInfo
import com.metrolist.music.wear.presentation.components.EqualizerIcon

/**
 * Screen showing the current playback queue.
 */
@Composable
fun QueueScreen(
    viewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = (uiState as? PlayerUiState.Ready)?.isPlaying ?: false

    val listState = rememberScalingLazyListState(
        initialCenterItemIndex = currentIndex.coerceAtLeast(0)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (queue.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Queue is empty",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    ListHeader {
                        Text("Queue (${queue.size})")
                    }
                }

                itemsIndexed(
                    items = queue,
                    key = { index, item -> "${item.mediaId}_$index" }
                ) { index, item ->
                    QueueItemCard(
                        item = item,
                        isPlaying = isPlaying && item.isCurrent,
                        onClick = {
                            if (item.isCurrent) {
                                // Navigate back to player when tapping current song
                                onNavigateToPlayer()
                            } else {
                                viewModel.playFromQueue(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(
    item: QueueItemInfo,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(item.artworkUri)
            .size(64)
            .crossfade(false)
            .memoryCacheKey(item.mediaId)
            .build()
    )

    // Highlight current item with primary color
    val buttonColors = if (item.isCurrent) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.filledTonalButtonColors()
    }

    Button(
        onClick = onClick,
        icon = {
            Box {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                // Show equalizer overlay for currently playing item
                if (item.isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        EqualizerIcon(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            animating = isPlaying
                        )
                    }
                }
            }
        },
        label = {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isCurrent) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
            )
        },
        secondaryLabel = {
            Text(
                text = item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.isCurrent) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                } else {
                    Color.Unspecified
                }
            )
        },
        colors = buttonColors,
        modifier = modifier.fillMaxWidth()
    )
}
