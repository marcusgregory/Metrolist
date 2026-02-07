package com.metrolist.music.wear.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.metrolist.music.wear.player.MediaPlayerAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the player screen.
 */
sealed class PlayerUiState {
    data object Loading : PlayerUiState()

    data class Ready(
        val title: String,
        val artist: String,
        val artworkUri: Uri?,
        val isPlaying: Boolean,
        val isBuffering: Boolean,
        val position: Long,
        val duration: Long
    ) : PlayerUiState() {
        val progress: Float
            get() = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    data class Error(val message: String) : PlayerUiState()
}

/**
 * ViewModel for the player screen.
 * Manages the MediaPlayerAdapter and exposes UI state as StateFlow.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private var adapter: MediaPlayerAdapter? = null
    private var positionUpdateJob: Job? = null

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /**
     * Attach a MediaController to this ViewModel.
     * Creates the adapter and starts collecting state.
     */
    fun attachController(controller: MediaController) {
        Timber.d("Attaching MediaController")

        // Release previous adapter if exists
        adapter?.release()

        adapter = MediaPlayerAdapter(controller).also { newAdapter ->
            collectAdapterState(newAdapter)
            startPositionUpdates(newAdapter)
        }
    }

    private fun collectAdapterState(adapter: MediaPlayerAdapter) {
        viewModelScope.launch {
            combine(
                adapter.isPlaying,
                adapter.currentMediaItem,
                adapter.position,
                adapter.duration,
                adapter.playbackState
            ) { isPlaying, mediaItem, position, duration, playbackState ->
                val isBuffering = playbackState == Player.STATE_BUFFERING
                if (mediaItem != null) {
                    PlayerUiState.Ready(
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        artist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                        artworkUri = mediaItem.mediaMetadata.artworkUri,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        position = position,
                        duration = duration
                    )
                } else {
                    PlayerUiState.Loading
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun startPositionUpdates(adapter: MediaPlayerAdapter) {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                adapter.updatePosition()
                delay(1000L) // Update every second
            }
        }
    }

    /**
     * Detach the controller and release resources.
     */
    fun detachController() {
        Timber.d("Detaching MediaController")
        positionUpdateJob?.cancel()
        positionUpdateJob = null
        adapter?.release()
        adapter = null
        _uiState.value = PlayerUiState.Loading
    }

    // Playback controls
    fun togglePlayPause() {
        Timber.d("togglePlayPause")
        adapter?.togglePlayPause()
    }

    fun play() {
        Timber.d("play")
        adapter?.play()
    }

    fun pause() {
        Timber.d("pause")
        adapter?.pause()
    }

    fun skipNext() {
        Timber.d("skipNext")
        adapter?.skipNext()
    }

    fun skipPrevious() {
        Timber.d("skipPrevious")
        adapter?.skipPrev()
    }

    fun seekTo(positionMs: Long) {
        Timber.d("seekTo: $positionMs")
        adapter?.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        detachController()
    }
}
