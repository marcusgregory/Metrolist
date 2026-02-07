package com.metrolist.music.wear.presentation

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.metrolist.music.wear.presentation.home.NowPlayingInfo
import com.metrolist.music.wear.data.model.WearAlbum
import com.metrolist.music.wear.data.model.WearArtist
import com.metrolist.music.wear.data.model.WearPlaylist
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
    private var musicService: WearMusicService? = null
    private var serviceBound = false

    // State flow to track if service has media (for faster "Now Playing" detection)
    private val _serviceHasMedia = kotlinx.coroutines.flow.MutableStateFlow(false)

    @Inject
    lateinit var historyRepository: HistoryRepository

    // ServiceConnection to check playback state directly
    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
            val binder = service as? WearMusicService.MusicBinder
            musicService = binder?.service
            serviceBound = true
            val hasMedia = musicService?.getPlayer()?.currentMediaItem != null
            _serviceHasMedia.value = hasMedia
            Timber.d("MusicService bound, hasMedia: $hasMedia")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
            _serviceHasMedia.value = false
        }
    }

    @OptIn(ExperimentalHorologistApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity onCreate")

        // Bind to service early to check playback state
        bindToMusicService()

        // Check if opened from notification (session activity)
        val openedFromNotification = intent?.action == Intent.ACTION_VIEW ||
            intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == false

        setContent {
            val uiState by playerViewModel.uiState.collectAsState()
            val serviceHasMedia by _serviceHasMedia.collectAsState()

            // Build NowPlayingInfo from either MediaController state or service
            val nowPlayingInfo: NowPlayingInfo? = when {
                uiState is PlayerUiState.Ready -> {
                    val readyState = uiState as PlayerUiState.Ready
                    NowPlayingInfo(
                        title = readyState.title,
                        artist = readyState.artist,
                        artworkUri = readyState.artworkUri,
                        isPlaying = readyState.isPlaying,
                        isBuffering = readyState.isBuffering
                    )
                }
                serviceHasMedia -> {
                    // Fallback: get basic info from service while MediaController connects
                    val player = musicService?.getPlayer()
                    val mediaItem = player?.currentMediaItem
                    mediaItem?.let {
                        NowPlayingInfo(
                            title = it.mediaMetadata.title?.toString() ?: "Unknown",
                            artist = it.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                            artworkUri = it.mediaMetadata.artworkUri,
                            isPlaying = player.isPlaying,
                            isBuffering = player.playbackState == androidx.media3.common.Player.STATE_BUFFERING
                        )
                    }
                }
                else -> null
            }

            WearTheme {
                // AmbientAware from Horologist handles ambient mode properly
                AmbientAware { ambientState ->
                    val isAmbient = ambientState is AmbientState.Ambient

                    WearNavigation(
                        playerViewModel = playerViewModel,
                        nowPlayingInfo = nowPlayingInfo,
                        isAmbient = isAmbient,
                        startOnPlayer = openedFromNotification,
                        onPlaySong = { song -> playSong(song) },
                        onPlayHistoryItem = { item -> playHistoryItem(item) },
                        onPlayAlbum = { album, songs -> playAlbum(album, songs) },
                        onPlayArtist = { artist, songs -> playArtist(artist, songs) },
                        onPlayPlaylist = { playlist, songs -> playPlaylist(playlist, songs) }
                    )
                }
            }
        }

        // Connect to MediaController
        connectToMediaController()
    }

    private fun bindToMusicService() {
        val intent = Intent(this, WearMusicService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
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
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
        musicService = null
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

    private fun playPlaylist(playlist: WearPlaylist, songs: List<WearSong>) {
        if (songs.isEmpty()) return

        Timber.d("Playing playlist: ${playlist.title} with ${songs.size} songs")

        // Add first song to history
        val firstSong = songs.first()
        lifecycleScope.launch {
            historyRepository.addToHistory(
                id = firstSong.id,
                title = firstSong.title,
                artist = firstSong.artist,
                thumbnailUrl = firstSong.thumbnailUrl
            )
        }

        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    songs.map { song ->
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.thumbnailUrl
                        )
                    }
                )
                unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }, BIND_AUTO_CREATE)
    }

    private fun playAlbum(album: WearAlbum, songs: List<WearSong>) {
        if (songs.isEmpty()) return

        Timber.d("Playing album: ${album.title} with ${songs.size} songs")

        val firstSong = songs.first()
        lifecycleScope.launch {
            historyRepository.addToHistory(
                id = firstSong.id,
                title = firstSong.title,
                artist = firstSong.artist,
                thumbnailUrl = firstSong.thumbnailUrl
            )
        }

        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    songs.map { song ->
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.thumbnailUrl
                        )
                    }
                )
                unbindService(this)
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }, BIND_AUTO_CREATE)
    }

    private fun playArtist(artist: WearArtist, songs: List<WearSong>) {
        if (songs.isEmpty()) return

        Timber.d("Playing artist: ${artist.name} with ${songs.size} songs")

        val firstSong = songs.first()
        lifecycleScope.launch {
            historyRepository.addToHistory(
                id = firstSong.id,
                title = firstSong.title,
                artist = firstSong.artist,
                thumbnailUrl = firstSong.thumbnailUrl
            )
        }

        val intent = android.content.Intent(this, WearMusicService::class.java)
        bindService(intent, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
                val binder = service as WearMusicService.MusicBinder
                binder.service.playQueue(
                    songs.map { song ->
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artist,
                            artworkUrl = song.thumbnailUrl
                        )
                    }
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
    nowPlayingInfo: NowPlayingInfo?,
    isAmbient: Boolean,
    startOnPlayer: Boolean = false,
    onPlaySong: (WearSong) -> Unit,
    onPlayHistoryItem: (HistoryEntity) -> Unit,
    onPlayAlbum: (WearAlbum, List<WearSong>) -> Unit,
    onPlayArtist: (WearArtist, List<WearSong>) -> Unit,
    onPlayPlaylist: (WearPlaylist, List<WearSong>) -> Unit
) {
    val navController = rememberSwipeDismissableNavController()

    // Remember start destination to avoid flicker on recomposition
    val startDestination = remember { if (startOnPlayer) "player" else "home" }

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("home") {
            HomeScreen(
                nowPlayingInfo = nowPlayingInfo,
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
                },
                onQueueClick = {
                    navController.navigate("queue") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("queue") {
            QueueScreen(
                viewModel = playerViewModel,
                onNavigateToPlayer = { navController.popBackStack() }
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
                },
                onAlbumPlay = { album, songs ->
                    onPlayAlbum(album, songs)
                    navController.navigate("player") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                onArtistPlay = { artist, songs ->
                    onPlayArtist(artist, songs)
                    navController.navigate("player") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                },
                onPlaylistPlay = { playlist, songs ->
                    onPlayPlaylist(playlist, songs)
                    navController.navigate("player") {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
