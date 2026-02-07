package com.metrolist.music.wear.presentation

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import com.google.common.util.concurrent.MoreExecutors
import com.metrolist.music.wear.data.db.HistoryEntity
import com.metrolist.music.wear.data.repository.HistoryRepository
import com.metrolist.music.wear.player.WearMusicService
import com.metrolist.music.wear.presentation.home.HomeScreen
import com.metrolist.music.wear.presentation.search.SearchScreen
import com.metrolist.music.wear.presentation.search.WearSong
import com.metrolist.music.wear.presentation.theme.WearTheme
import com.metrolist.music.wear.presentation.volume.WearVolumeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private var mediaController: MediaController? = null

    @Inject
    lateinit var historyRepository: HistoryRepository

    @OptIn(ExperimentalHorologistApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        setContent {
            val uiState by playerViewModel.uiState.collectAsState()
            val hasNowPlaying = uiState is PlayerUiState.Ready

            WearTheme {
                // AmbientAware from Horologist handles ambient mode properly
                AmbientAware { ambientState ->
                    val isAmbient = ambientState is AmbientState.Ambient

                    WearNavigation(
                        playerViewModel = playerViewModel,
                        hasNowPlaying = hasNowPlaying,
                        isAmbient = isAmbient,
                        onPlaySong = { song -> playSong(song) },
                        onPlayHistoryItem = { item -> playHistoryItem(item) }
                    )
                }
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

    private fun playSong(song: WearSong) {
        Timber.d("Playing song: ${song.title}")

        // Add to history
        lifecycleScope.launch {
            historyRepository.addToHistory(
                id = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl
            )
        }

        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    listOf(
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artist
                        )
                    )
                )
                unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }, BIND_AUTO_CREATE)
    }

    private fun playHistoryItem(item: HistoryEntity) {
        Timber.d("Playing from history: ${item.title}")

        // Update timestamp in history
        lifecycleScope.launch {
            historyRepository.addToHistory(
                id = item.id,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl
            )
        }

        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    listOf(
                        WearMusicService.QueueItem(
                            videoId = item.id,
                            title = item.title,
                            artist = item.artist
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
    hasNowPlaying: Boolean,
    isAmbient: Boolean,
    onPlaySong: (WearSong) -> Unit,
    onPlayHistoryItem: (HistoryEntity) -> Unit
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                hasNowPlaying = hasNowPlaying,
                isAmbient = isAmbient,
                onSearchClick = {
                    navController.navigate("search") {
                        launchSingleTop = true
                    }
                },
                onNowPlayingClick = {
                    navController.navigate("player") {
                        launchSingleTop = true
                    }
                },
                onHistoryItemClick = { item ->
                    onPlayHistoryItem(item)
                    navController.navigate("player") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("player") {
            PlayerScreen(
                viewModel = playerViewModel,
                isAmbient = isAmbient,
                modifier = Modifier,
                onSearchClick = {
                    navController.navigate("search") {
                        launchSingleTop = true
                    }
                },
                onVolumeClick = {
                    navController.navigate("volume") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("volume") {
            WearVolumeScreen()
        }
        composable("search") {
            SearchScreen(
                onSongClick = { song ->
                    onPlaySong(song)
                    navController.navigate("player") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
