package com.metrolist.music.wear.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.metrolist.music.wear.player.WearMusicService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var musicService: WearMusicService? = null
    private var bound = false
    private var serviceState = mutableStateOf<WearMusicService?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("Service connected")
            val binder = service as WearMusicService.MusicBinder
            musicService = binder.service
            serviceState.value = binder.service
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("Service disconnected")
            musicService = null
            serviceState.value = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        // Bind to service
        Intent(this, WearMusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            WearApp(
                service = serviceState.value,
                onPlayTest = { playTestVideo() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    private fun playTestVideo() {
        Timber.d("Playing test video")
        musicService?.playByVideoId(
            videoId = "dQw4w9WgXcQ", // Rick Astley - Never Gonna Give You Up
            title = "Never Gonna Give You Up",
            artist = "Rick Astley"
        )
    }
}

@Composable
fun WearApp(
    service: WearMusicService?,
    onPlayTest: () -> Unit
) {
    var status by remember { mutableStateOf("Connecting...") }
    var isPlaying by remember { mutableStateOf(false) }

    // Update status when service connects
    LaunchedEffect(service) {
        status = if (service != null) "Ready to play" else "Connecting..."
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Metrolist Wear",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (service == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Button(
                    onClick = {
                        onPlayTest()
                        isPlaying = true
                        status = "Playing..."
                    }
                ) {
                    Text(if (isPlaying) "Playing..." else "Play Test")
                }

                if (isPlaying) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            service.getPlayer().let { player ->
                                if (player.isPlaying) {
                                    player.pause()
                                    status = "Paused"
                                } else {
                                    player.play()
                                    status = "Playing..."
                                }
                            }
                        }
                    ) {
                        Text("Play/Pause")
                    }
                }
            }
        }
    }
}
