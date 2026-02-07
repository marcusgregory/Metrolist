package com.metrolist.music.wear.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.wear.data.model.WearAlbum
import com.metrolist.music.wear.data.model.WearArtist
import com.metrolist.music.wear.data.model.WearPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Unified search results containing all types.
 */
data class UnifiedSearchResults(
    val songs: List<WearSong> = emptyList(),
    val albums: List<WearAlbum> = emptyList(),
    val artists: List<WearArtist> = emptyList(),
    val playlists: List<WearPlaylist> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()

    val totalCount: Int
        get() = songs.size + albums.size + artists.size + playlists.size
}

/**
 * Search UI state.
 */
sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Results(val results: UnifiedSearchResults) : SearchState()
    data object LoadingContent : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WearSearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var lastQuery: String = ""

    /**
     * Perform unified search for all content types in parallel.
     */
    fun search(query: String) {
        Timber.d("Unified search for: $query")
        lastQuery = query
        viewModelScope.launch {
            _state.value = SearchState.Loading

            // Run all searches in parallel
            val songsDeferred = async { repository.search(query) }
            val albumsDeferred = async { repository.searchAlbums(query) }
            val artistsDeferred = async { repository.searchArtists(query) }
            val playlistsDeferred = async { repository.searchPlaylists(query) }

            // Await all results
            val songs = songsDeferred.await().getOrElse { emptyList() }
            val albums = albumsDeferred.await().getOrElse { emptyList() }
            val artists = artistsDeferred.await().getOrElse { emptyList() }
            val playlists = playlistsDeferred.await().getOrElse { emptyList() }

            val results = UnifiedSearchResults(
                songs = songs.take(10),
                albums = albums.take(5),
                artists = artists.take(5),
                playlists = playlists.take(10)
            )

            Timber.d("Unified search results: ${results.songs.size} songs, ${results.albums.size} albums, ${results.artists.size} artists, ${results.playlists.size} playlists")

            if (results.isEmpty) {
                _state.value = SearchState.Error("No results found")
            } else {
                _state.value = SearchState.Results(results)
            }
        }
    }

    /**
     * Load songs from a playlist and return them.
     */
    suspend fun loadPlaylistSongs(playlistId: String): List<WearSong> {
        Timber.d("Loading playlist songs: $playlistId")
        _state.value = SearchState.LoadingContent
        return repository.getPlaylistSongs(playlistId)
            .onFailure { error ->
                Timber.e(error, "Failed to load playlist songs")
                _state.value = SearchState.Error(error.message ?: "Failed to load playlist")
            }
            .getOrElse { emptyList() }
    }

    /**
     * Load songs from an album and return them.
     */
    suspend fun loadAlbumSongs(albumId: String): List<WearSong> {
        Timber.d("Loading album songs: $albumId")
        _state.value = SearchState.LoadingContent
        return repository.getPlaylistSongs(albumId)
            .onFailure { error ->
                Timber.e(error, "Failed to load album songs")
                _state.value = SearchState.Error(error.message ?: "Failed to load album")
            }
            .getOrElse { emptyList() }
    }

    /**
     * Load songs from an artist and return them.
     */
    suspend fun loadArtistSongs(artistId: String): List<WearSong> {
        Timber.d("Loading artist songs: $artistId")
        _state.value = SearchState.LoadingContent
        return repository.getArtistSongs(artistId)
            .onFailure { error ->
                Timber.e(error, "Failed to load artist songs")
                _state.value = SearchState.Error(error.message ?: "Failed to load artist")
            }
            .getOrElse { emptyList() }
    }

    /**
     * Retry the last search.
     */
    fun retry() {
        if (lastQuery.isNotBlank()) {
            search(lastQuery)
        }
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        _state.value = SearchState.Idle
        lastQuery = ""
    }
}
