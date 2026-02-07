package com.metrolist.music.wear.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.data.db.HistoryEntity
import com.metrolist.music.wear.presentation.SearchIcon

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    nowPlayingInfo: NowPlayingInfo?,
    isAmbient: Boolean = false,
    onSearchClick: () -> Unit,
    onNowPlayingClick: () -> Unit,
    onHistoryItemClick: (HistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentHistory by viewModel.recentHistory.collectAsState(initial = emptyList())
    val listState = rememberScalingLazyListState()

    // In ambient mode, use simplified colors for burn-in protection
    val textColor = if (isAmbient) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary

    ScalingLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        state = listState
    ) {
        // Header
        item {
            ListHeader {
                Text(
                    text = "Metrolist",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
        }

        // In ambient mode, only show minimal info
        if (isAmbient) {
            // In ambient mode, just show song title if there's music
            nowPlayingInfo?.let { info ->
                item {
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            return@ScalingLazyColumn
        }

        // Search Button
        item {
            Button(
                onClick = onSearchClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = SearchIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text("Search")
            }
        }

        // Now Playing chip with song info, thumbnail, and animated equalizer
        nowPlayingInfo?.let { info ->
            item {
                NowPlayingChip(
                    info = info,
                    onClick = onNowPlayingClick
                )
            }
        }

        // Recent History
        if (recentHistory.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Recent")
                }
            }

            items(recentHistory, key = { it.id }) { item ->
                HistoryItem(
                    item = item,
                    onClick = { onHistoryItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    item: HistoryEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(item.thumbnailUrl)
            .size(64)
            .crossfade(false)
            .memoryCacheKey(item.id)
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
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.fillMaxWidth()
    )
}
