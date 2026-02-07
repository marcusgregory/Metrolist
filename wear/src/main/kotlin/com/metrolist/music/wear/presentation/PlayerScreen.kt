package com.metrolist.music.wear.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    isAmbient: Boolean = false,
    onSearchClick: () -> Unit = {},
    onVolumeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                LoadingContent(onSearchClick = onSearchClick)
            }
            is PlayerUiState.Ready -> {
                PlayerContent(
                    state = state,
                    isAmbient = isAmbient,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipNext,
                    onPrevious = viewModel::skipPrevious,
                    onSearchClick = onSearchClick,
                    onVolumeClick = onVolumeClick,
                    onSeek = { offsetMs ->
                        // Get current position from uiState (not stale state)
                        val currentState = viewModel.uiState.value as? PlayerUiState.Ready
                        val currentPosition = currentState?.position ?: state.position
                        val duration = currentState?.duration ?: state.duration
                        val newPosition = (currentPosition + offsetMs).coerceIn(0L, duration)
                        viewModel.seekTo(newPosition)
                    }
                )
            }
            is PlayerUiState.Error -> {
                ErrorContent(message = state.message)
            }
        }
    }
}

@Composable
private fun LoadingContent(
    onSearchClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Metrolist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No music playing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.wear.compose.material3.Button(
            onClick = onSearchClick
        ) {
            Icon(
                imageVector = SearchIcon,
                contentDescription = null,
                modifier = Modifier.requiredSize(16.dp)
            )
            Spacer(modifier = Modifier.requiredSize(4.dp))
            Text("Search")
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState.Ready,
    isAmbient: Boolean = false,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSearchClick: () -> Unit,
    onVolumeClick: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    // In ambient mode, use simplified colors for burn-in protection
    val textColor = if (isAmbient) Color.White.copy(alpha = 0.6f) else Color.White
    val secondaryTextColor = if (isAmbient) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.7f)
    val buttonBgColor = if (isAmbient) Color.Transparent else Color.White.copy(alpha = 0.9f)
    val buttonIconColor = if (isAmbient) Color.White.copy(alpha = 0.6f) else Color.Black

    // Track seek state
    var seekOffsetMs by remember { mutableLongStateOf(0L) }
    var seekJob by remember { mutableStateOf<Job?>(null) }

    // Calculate preview progress while seeking
    val isSeeking = seekOffsetMs != 0L
    val previewProgress = if (isSeeking) {
        val previewPosition = (state.position + seekOffsetMs).coerceIn(0L, state.duration)
        if (state.duration > 0) (previewPosition.toFloat() / state.duration).coerceIn(0f, 1f) else 0f
    } else {
        state.progress
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background artwork with dark overlay (hide in ambient mode for burn-in protection)
        if (!isAmbient) {
            state.artworkUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                )
            }
        }

        // TimeText at top (curved)
        TimeText()

        // All content in one centered Column
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            // Artist
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controls - filled buttons, smaller (simplified in ambient mode)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Previous - filled (tap = skip, long-press = seek backward)
                Box(
                    modifier = Modifier
                        .requiredSize(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAmbient) Color.Transparent
                            else if (seekOffsetMs < 0L) Color.White.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.9f)
                        )
                        .pointerInput(isAmbient) {
                            if (!isAmbient) {
                                detectTapGestures(
                                    onTap = { onPrevious() },
                                    onLongPress = {
                                        // Start accumulating seek offset
                                        seekJob = scope.launch {
                                            while (true) {
                                                seekOffsetMs -= 5000L
                                                delay(300L)
                                            }
                                        }
                                    },
                                    onPress = {
                                        tryAwaitRelease()
                                        // On release: cancel job and apply seek
                                        seekJob?.cancel()
                                        seekJob = null
                                        if (seekOffsetMs < 0L) {
                                            onSeek(seekOffsetMs)
                                            seekOffsetMs = 0L
                                        }
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SkipPreviousIcon,
                        contentDescription = "Previous (hold to rewind)",
                        tint = buttonIconColor,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }

                // Play/Pause - with progress ring around it
                val progressAngle = 360f * previewProgress
                val ringColor = if (isAmbient) Color.White.copy(alpha = 0.3f) else Color.White
                val ringTrackColor = if (isAmbient) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .requiredSize(64.dp) // Slightly larger to accommodate progress ring
                        .drawBehind {
                            val strokeWidth = 4.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f

                            // Track (background ring)
                            drawCircle(
                                color = ringTrackColor,
                                radius = radius,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Progress arc (yellow tint when seeking to show preview)
                            if (progressAngle > 0f) {
                                drawArc(
                                    color = if (isSeeking) Color(0xFFFFD700) else ringColor,
                                    startAngle = -90f,
                                    sweepAngle = progressAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner button
                    Box(
                        modifier = Modifier
                            .requiredSize(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAmbient) Color.Transparent
                                else if (state.isBuffering) Color.White.copy(alpha = 0.7f)
                                else Color.White
                            )
                            .clickable(enabled = !state.isBuffering && !isAmbient) { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering && !isAmbient) {
                            CircularProgressIndicator(
                                modifier = Modifier.requiredSize(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) PauseIcon else PlayIcon,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = buttonIconColor,
                                modifier = Modifier.requiredSize(28.dp)
                            )
                        }
                    }
                }

                // Next - filled (tap = skip, long-press = seek forward)
                Box(
                    modifier = Modifier
                        .requiredSize(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAmbient) Color.Transparent
                            else if (seekOffsetMs > 0L) Color.White.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.9f)
                        )
                        .pointerInput(isAmbient) {
                            if (!isAmbient) {
                                detectTapGestures(
                                    onTap = { onNext() },
                                    onLongPress = {
                                        // Start accumulating seek offset
                                        seekJob = scope.launch {
                                            while (true) {
                                                seekOffsetMs += 5000L
                                                delay(300L)
                                            }
                                        }
                                    },
                                    onPress = {
                                        tryAwaitRelease()
                                        // On release: cancel job and apply seek
                                        seekJob?.cancel()
                                        seekJob = null
                                        if (seekOffsetMs > 0L) {
                                            onSeek(seekOffsetMs)
                                            seekOffsetMs = 0L
                                        }
                                    }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SkipNextIcon,
                        contentDescription = "Next (hold to fast-forward)",
                        tint = buttonIconColor,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
            }

            // Bottom buttons row (hide in ambient mode)
            if (!isAmbient) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume button
                    Box(
                        modifier = Modifier
                            .requiredSize(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onVolumeClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VolumeIcon,
                            contentDescription = "Volume & Audio Output",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.requiredSize(18.dp)
                        )
                    }

                    // Search button
                    Box(
                        modifier = Modifier
                            .requiredSize(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onSearchClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SearchIcon,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.requiredSize(18.dp)
                        )
                    }
                }
            }
        }
    }
}
