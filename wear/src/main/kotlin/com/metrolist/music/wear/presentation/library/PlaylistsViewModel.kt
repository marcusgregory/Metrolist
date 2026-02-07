package com.metrolist.music.wear.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.wear.auth.LibraryPlaylist
import com.metrolist.music.wear.auth.LibraryService
import com.metrolist.music.wear.presentation.search.WearSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class PlaylistsState {
    data object Loading : PlaylistsState()
    data class Success(val playlists: List<LibraryPlaylist>) : PlaylistsState()
    data class Error(val message: String) : PlaylistsState()
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val libraryService: LibraryService
) : ViewModel() {

    private val _state = MutableStateFlow<PlaylistsState>(PlaylistsState.Loading)
    val state: StateFlow<PlaylistsState> = _state.asStateFlow()

    private val _isLoadingPlaylist = MutableStateFlow(false)
    val isLoadingPlaylist: StateFlow<Boolean> = _isLoadingPlaylist.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _state.value = PlaylistsState.Loading
            libraryService.getUserPlaylists()
                .onSuccess { playlists ->
                    Timber.d("Loaded ${playlists.size} playlists")
                    _state.value = PlaylistsState.Success(playlists)
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load playlists")
                    _state.value = PlaylistsState.Error(error.message ?: "Unknown error")
                }
        }
    }

    /**
     * Load songs from a user's library playlist using TV client (requires auth).
     * For public playlists from search, use WearSearchRepository instead.
     */
    suspend fun loadPlaylistSongs(playlistId: String): List<WearSong> {
        _isLoadingPlaylist.value = true
        return try {
            // Use LibraryService for user's library playlists (TV client with OAuth)
            libraryService.getPlaylistSongs(playlistId)
                .onFailure { error ->
                    Timber.e(error, "Failed to load playlist songs")
                }
                .getOrElse { emptyList() }
        } finally {
            _isLoadingPlaylist.value = false
        }
    }

    fun retry() {
        loadPlaylists()
    }
}
