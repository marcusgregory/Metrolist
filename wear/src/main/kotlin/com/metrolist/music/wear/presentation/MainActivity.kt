package com.metrolist.music.wear.presentation

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.common.util.concurrent.MoreExecutors
import com.metrolist.music.wear.player.WearMusicService
import com.metrolist.music.wear.presentation.theme.WearTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        setContent {
            WearTheme {
                WearNavigation(
                    playerViewModel = playerViewModel,
                    onPlayTest = { playTestVideo() }
                )
            }
        }

        // Connect to MediaController
        connectToMediaController()
    }

    private fun connectToMediaController() {
        val sessionToken = SessionToken(
            this,
            ComponentName(this, WearMusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                Timber.d("MediaController connected")
                mediaController?.let { controller ->
                    playerViewModel.attachController(controller)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect MediaController")
            }
        }, MoreExecutors.directExecutor())

        // Also observe lifecycle to reattach when resuming
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mediaController?.let { controller ->
                    playerViewModel.attachController(controller)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity onDestroy")
        playerViewModel.detachController()
        mediaController?.release()
        mediaController = null
    }

    private fun playTestVideo() {
        Timber.d("Playing test video via MediaController")
        // For testing, we need to access the service directly
        // In production, this would use MediaController commands
        val sessionToken = SessionToken(
            this,
            ComponentName(this, WearMusicService::class.java)
        )

        // Bind to service to play queue
        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    listOf(
                        WearMusicService.QueueItem(
                            videoId = "07JkrRIPr_w",
                            title = "Test Song 1",
                            artist = "Artist 1"
                        ),
                        WearMusicService.QueueItem(
                            videoId = "dQw4w9WgXcQ",
                            title = "Never Gonna Give You Up",
                            artist = "Rick Astley"
                        )
                    )
                )
                unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }, BIND_AUTO_CREATE)
    }
}

@Composable
fun WearNavigation(
    playerViewModel: PlayerViewModel,
    onPlayTest: () -> Unit
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "player"
    ) {
        composable("player") {
            PlayerScreen(
                viewModel = playerViewModel,
                modifier = Modifier,
                onPlayTest = onPlayTest
            )
        }
        // Phase 3: Add more destinations
        // composable("search") { SearchScreen() }
    }
}
