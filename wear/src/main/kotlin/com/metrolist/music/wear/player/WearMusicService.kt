package com.metrolist.music.wear.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.metrolist.innertube.YouTube
import com.metrolist.music.wear.constants.AudioQuality
import com.metrolist.music.wear.di.PlayerCache
import com.metrolist.music.wear.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class WearMusicService : MediaLibraryService() {

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val binder = MusicBinder()

    // Cache de URLs em memoria (videoId -> (url, expirationTime))
    private val songUrlCache = mutableMapOf<String, Pair<String, Long>>()

    private val connectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }

    inner class MusicBinder : Binder() {
        val service: WearMusicService
            get() = this@WearMusicService
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onCreate() {
        super.onCreate()
        Timber.d("WearMusicService onCreate")

        createNotificationChannel()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(createDataSourceFactory())
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()

        mediaSession = MediaLibrarySession.Builder(this, player, MediaLibrarySessionCallback())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build()
        )

        Timber.d("WearMusicService initialized successfully")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        Timber.d("WearMusicService onDestroy")
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val okHttpClient = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()

        val cacheFactory = CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this,
                    OkHttpDataSource.Factory(okHttpClient)
                )
            )

        return ResolvingDataSource.Factory(cacheFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            // Verificar cache de URL em memoria
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                Timber.d("Using cached URL for $mediaId")
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // Resolver URL via YTPlayerUtils
            Timber.d("Resolving URL for $mediaId")
            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaId,
                    audioQuality = AudioQuality.AUTO,
                    connectivityManager = connectivityManager,
                )
            }.getOrElse { throwable ->
                Timber.e(throwable, "Failed to resolve URL for $mediaId")
                throw PlaybackException(
                    throwable.message ?: "Unknown error",
                    throwable,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }

            // Cachear URL
            val expirationTime = System.currentTimeMillis() + playbackData.streamExpiresInSeconds * 1000L
            songUrlCache[mediaId] = playbackData.streamUrl to expirationTime
            Timber.d("Cached URL for $mediaId, expires in ${playbackData.streamExpiresInSeconds}s")

            dataSpec.withUri(playbackData.streamUrl.toUri())
        }
    }

    /**
     * Play a YouTube video by its ID.
     * Used by the UI to start playback.
     */
    fun playByVideoId(videoId: String, title: String = "", artist: String = "", artworkUrl: String? = null) {
        Timber.d("playByVideoId: $videoId, title: $title, artist: $artist")
        val mediaItem = buildMediaItem(videoId, title, artist, artworkUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    /**
     * Play a list of videos as a queue.
     */
    fun playQueue(items: List<QueueItem>) {
        Timber.d("playQueue: ${items.size} items")
        val mediaItems = items.map { buildMediaItem(it.videoId, it.title, it.artist, it.artworkUrl) }
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    private fun buildMediaItem(videoId: String, title: String, artist: String, artworkUrl: String?): MediaItem {
        val artwork = artworkUrl ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri("https://music.youtube.com/watch?v=$videoId")
            .setCustomCacheKey(videoId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title.ifEmpty { videoId })
                    .setArtist(artist.ifEmpty { "Unknown Artist" })
                    .setArtworkUri(artwork.toUri())
                    .build()
            )
            .build()
    }

    data class QueueItem(
        val videoId: String,
        val title: String = "",
        val artist: String = "",
        val artworkUrl: String? = null
    )

    /**
     * Get the ExoPlayer instance for direct control.
     */
    fun getPlayer(): ExoPlayer = player

    private inner class MediaLibrarySessionCallback : MediaLibrarySession.Callback {
        // Default implementation for now
        // Can be extended for custom media browser functionality
    }

    companion object {
        const val CHANNEL_ID = "wear_music_channel"
        const val NOTIFICATION_ID = 1001
    }
}
