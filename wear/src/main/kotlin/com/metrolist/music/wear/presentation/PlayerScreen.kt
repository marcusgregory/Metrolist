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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onPlayTest: (() -> Unit)? = null
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
                LoadingContent(onPlayTest = onPlayTest)
            }
            is PlayerUiState.Ready -> {
                PlayerContent(
                    state = state,
                    onPlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipNext,
                    onPrevious = viewModel::skipPrevious,
                    onSeekForward = { viewModel.seekTo(state.position + 5000L) },
                    onSeekBackward = { viewModel.seekTo((state.position - 5000L).coerceAtLeast(0L)) }
                )
            }
            is PlayerUiState.Error -> {
                ErrorContent(message = state.message)
            }
        }
    }
}

@Composable
private fun LoadingContent(onPlayTest: (() -> Unit)? = null) {
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
        if (onPlayTest != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.wear.compose.material3.Button(
                onClick = onPlayTest
            ) {
                Text("Play Test")
            }
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
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit
) {
    // Track long-press state for continuous seeking
    var isLongPressingNext by remember { mutableStateOf(false) }
    var isLongPressingPrev by remember { mutableStateOf(false) }

    // Continuous seek while long-pressing
    LaunchedEffect(isLongPressingNext) {
        while (isLongPressingNext) {
            onSeekForward()
            delay(200L) // Seek every 200ms while holding
        }
    }

    LaunchedEffect(isLongPressingPrev) {
        while (isLongPressingPrev) {
            onSeekBackward()
            delay(200L) // Seek every 200ms while holding
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background artwork with dark overlay
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            // Artist
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controls - filled buttons, smaller
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
                            if (isLongPressingPrev) Color.White.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.9f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onPrevious() },
                                onLongPress = { isLongPressingPrev = true },
                                onPress = {
                                    tryAwaitRelease()
                                    isLongPressingPrev = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SkipPreviousIcon,
                        contentDescription = "Previous (hold to rewind)",
                        tint = Color.Black,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }

                // Play/Pause - with progress ring around it
                val progressAngle = 360f * state.progress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .requiredSize(64.dp) // Slightly larger to accommodate progress ring
                        .drawBehind {
                            val strokeWidth = 4.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2f

                            // Track (background ring)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = radius,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Progress arc
                            if (progressAngle > 0f) {
                                drawArc(
                                    color = Color.White,
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
                                if (state.isBuffering) Color.White.copy(alpha = 0.7f)
                                else Color.White
                            )
                            .clickable(enabled = !state.isBuffering) { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.requiredSize(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) PauseIcon else PlayIcon,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
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
                            if (isLongPressingNext) Color.White.copy(alpha = 0.7f)
                            else Color.White.copy(alpha = 0.9f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onNext() },
                                onLongPress = { isLongPressingNext = true },
                                onPress = {
                                    tryAwaitRelease()
                                    isLongPressingNext = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SkipNextIcon,
                        contentDescription = "Next (hold to fast-forward)",
                        tint = Color.Black,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
            }
        }
    }
}
