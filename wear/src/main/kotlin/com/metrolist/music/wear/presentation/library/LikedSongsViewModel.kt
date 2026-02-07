package com.metrolist.music.wear.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.wear.auth.LibraryService
import com.metrolist.music.wear.presentation.search.WearSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class LikedSongsState {
    data object Loading : LikedSongsState()
    data class Success(val songs: List<WearSong>) : LikedSongsState()
    data class Error(val message: String) : LikedSongsState()
}

@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val libraryService: LibraryService
) : ViewModel() {

    private val _state = MutableStateFlow<LikedSongsState>(LikedSongsState.Loading)
    val state: StateFlow<LikedSongsState> = _state.asStateFlow()

    init {
        loadLikedSongs()
    }

    fun loadLikedSongs() {
        viewModelScope.launch {
            _state.value = LikedSongsState.Loading
            libraryService.getLikedSongs()
                .onSuccess { songs ->
                    Timber.d("Loaded ${songs.size} liked songs")
                    _state.value = LikedSongsState.Success(songs)
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to load liked songs")
                    _state.value = LikedSongsState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun retry() {
        loadLikedSongs()
    }
}
