package com.metrolist.music.wear.presentation.volume

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.AudioOutput

// Simple minus icon for volume down
private val VolumeDownIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Minus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(5f, 11f)
            horizontalLineTo(19f)
            verticalLineTo(13f)
            horizontalLineTo(5f)
            close()
        }
    }.build()

// Simple plus icon for volume up
private val VolumeUpIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Plus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(11f, 5f)
            verticalLineTo(11f)
            horizontalLineTo(5f)
            verticalLineTo(13f)
            horizontalLineTo(11f)
            verticalLineTo(19f)
            horizontalLineTo(13f)
            verticalLineTo(13f)
            horizontalLineTo(19f)
            verticalLineTo(11f)
            horizontalLineTo(13f)
            verticalLineTo(5f)
            close()
        }
    }.build()

// Bluetooth icon
private val BluetoothIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Bluetooth",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(17.71f, 7.71f)
            lineTo(12f, 2f)
            horizontalLineTo(11f)
            verticalLineTo(9.59f)
            lineTo(6.41f, 5f)
            lineTo(5f, 6.41f)
            lineTo(10.59f, 12f)
            lineTo(5f, 17.59f)
            lineTo(6.41f, 19f)
            lineTo(11f, 14.41f)
            verticalLineTo(22f)
            horizontalLineTo(12f)
            lineTo(17.71f, 16.29f)
            lineTo(13.41f, 12f)
            close()
            moveTo(13f, 5.83f)
            lineTo(14.88f, 7.71f)
            lineTo(13f, 9.59f)
            close()
            moveTo(14.88f, 16.29f)
            lineTo(13f, 18.17f)
            verticalLineTo(14.41f)
            close()
        }
    }.build()

// Speaker icon
private val SpeakerIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Speaker",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(3f, 9f)
            verticalLineTo(15f)
            horizontalLineTo(7f)
            lineTo(12f, 20f)
            verticalLineTo(4f)
            lineTo(7f, 9f)
            close()
        }
    }.build()

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun WearVolumeScreen(
    viewModel: VolumeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val audioOutput by viewModel.audioOutput.collectAsState()
    val volumeState by viewModel.volumeState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Title
            Text(
                text = "Audio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Current output device
            AudioOutputChip(
                audioOutput = audioOutput,
                onClick = { viewModel.launchOutputSelection() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Volume controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volume down
                Box(
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { viewModel.decreaseVolume() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = VolumeDownIcon,
                        contentDescription = "Volume down",
                        tint = Color.White,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }

                // Volume level
                Text(
                    text = "${volumeState.current}",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White
                )

                // Volume up
                Box(
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { viewModel.increaseVolume() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = VolumeUpIcon,
                        contentDescription = "Volume up",
                        tint = Color.White,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Volume hint
            Text(
                text = "Max: ${volumeState.max}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun AudioOutputChip(
    audioOutput: AudioOutput,
    onClick: () -> Unit
) {
    val (icon, name) = when (audioOutput) {
        is AudioOutput.BluetoothHeadset -> BluetoothIcon to audioOutput.name
        is AudioOutput.WatchSpeaker -> SpeakerIcon to "Watch Speaker"
        is AudioOutput.None -> SpeakerIcon to "No output"
        else -> SpeakerIcon to "Unknown"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.requiredSize(20.dp)
            )
            Spacer(modifier = Modifier.requiredSize(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
