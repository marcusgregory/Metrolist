package com.metrolist.music.wear.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Represents an item in the playback queue.
 */
data class QueueItemInfo(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val index: Int,
    val isCurrent: Boolean
)

/**
 * Adapts Media3 MediaController callbacks to StateFlow for Compose consumption.
 * This is a lightweight adapter that translates imperative callbacks into reactive flows.
 */
class MediaPlayerAdapter(private val controller: MediaController) {

    private val _isPlaying = MutableStateFlow(controller.isPlaying)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow(controller.currentMediaItem)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _position = MutableStateFlow(controller.currentPosition.coerceAtLeast(0L))
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(controller.duration.coerceAtLeast(0L))
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackState = MutableStateFlow(controller.playbackState)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueItemInfo>>(emptyList())
    val queue: StateFlow<List<QueueItemInfo>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(controller.currentMediaItemIndex)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Timber.d("onIsPlayingChanged: $isPlaying")
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Timber.d("onMediaItemTransition: ${mediaItem?.mediaId}, reason: $reason")
            _currentMediaItem.value = mediaItem
            _duration.value = controller.duration.coerceAtLeast(0L)
            _currentIndex.value = controller.currentMediaItemIndex
            updateQueue()
        }

        override fun onPlaybackStateChanged(state: Int) {
            Timber.d("onPlaybackStateChanged: $state")
            _playbackState.value = state
            if (state == Player.STATE_READY) {
                _duration.value = controller.duration.coerceAtLeast(0L)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _position.value = newPosition.positionMs.coerceAtLeast(0L)
            _currentIndex.value = controller.currentMediaItemIndex
            updateQueue()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            Timber.d("onTimelineChanged: mediaItemCount=${controller.mediaItemCount}, reason=$reason")
            updateQueue()
        }
    }

    init {
        controller.addListener(listener)
        // Initialize with current values
        _duration.value = controller.duration.coerceAtLeast(0L)
        _position.value = controller.currentPosition.coerceAtLeast(0L)
        _currentIndex.value = controller.currentMediaItemIndex
        updateQueue()
    }

    private fun updateQueue() {
        val currentIdx = controller.currentMediaItemIndex
        val items = mutableListOf<QueueItemInfo>()

        for (i in 0 until controller.mediaItemCount) {
            val mediaItem = controller.getMediaItemAt(i)
            items.add(
                QueueItemInfo(
                    mediaId = mediaItem.mediaId,
                    title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    artworkUri = mediaItem.mediaMetadata.artworkUri,
                    index = i,
                    isCurrent = i == currentIdx
                )
            )
        }

        _queue.value = items
        Timber.d("Queue updated: ${items.size} items, current index: $currentIdx")
    }

    fun release() {
        controller.removeListener(listener)
    }

    fun updatePosition() {
        _position.value = controller.currentPosition.coerceAtLeast(0L)
    }

    // Playback controls
    fun togglePlayPause() {
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun play() = controller.play()
    fun pause() = controller.pause()
    fun skipNext() = controller.seekToNext()
    fun skipPrev() = controller.seekToPrevious()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun seekToIndex(index: Int) {
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
        }
    }

    fun removeFromQueue(index: Int) {
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
        }
    }

    // Helper properties
    val title: String?
        get() = currentMediaItem.value?.mediaMetadata?.title?.toString()

    val artist: String?
        get() = currentMediaItem.value?.mediaMetadata?.artist?.toString()

    val artworkUri: Uri?
        get() = currentMediaItem.value?.mediaMetadata?.artworkUri
}
