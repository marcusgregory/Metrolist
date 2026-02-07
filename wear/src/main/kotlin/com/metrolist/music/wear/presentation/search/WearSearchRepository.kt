package com.metrolist.music.wear.presentation.search

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.wear.data.model.WearAlbum
import com.metrolist.music.wear.data.model.WearArtist
import com.metrolist.music.wear.data.model.WearPlaylist
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for searching songs and playlists via YouTube/Innertube API.
 */
@Singleton
class WearSearchRepository @Inject constructor() {

    /**
     * Search for songs.
     */
    suspend fun search(query: String): Result<List<WearSong>> = runCatching {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
        result.items.filterIsInstance<SongItem>().map { song ->
            WearSong(
                id = song.id,
                title = song.title,
                artist = song.artists.joinToString { it.name },
                thumbnailUrl = song.thumbnail,
                duration = song.duration
            )
        }
    }

    /**
     * Search for albums.
     */
    suspend fun searchAlbums(query: String): Result<List<WearAlbum>> = runCatching {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrThrow()
        result.items.filterIsInstance<AlbumItem>().map { album ->
            WearAlbum(
                id = album.playlistId,
                title = album.title,
                artist = album.artists?.joinToString { it.name },
                year = album.year,
                thumbnailUrl = album.thumbnail
            )
        }
    }

    /**
     * Search for artists.
     */
    suspend fun searchArtists(query: String): Result<List<WearArtist>> = runCatching {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrThrow()
        result.items.filterIsInstance<ArtistItem>().map { artist ->
            WearArtist(
                id = artist.id,
                name = artist.title,
                thumbnailUrl = artist.thumbnail,
                subscriberCount = null
            )
        }
    }

    /**
     * Search for playlists (featured + community).
     */
    suspend fun searchPlaylists(query: String): Result<List<WearPlaylist>> = runCatching {
        // Search featured playlists
        val featured = YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
            .getOrNull()?.items?.filterIsInstance<PlaylistItem>() ?: emptyList()

        // Search community playlists
        val community = YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
            .getOrNull()?.items?.filterIsInstance<PlaylistItem>() ?: emptyList()

        // Combine and deduplicate by ID
        (featured + community)
            .distinctBy { it.id }
            .map { playlist ->
                WearPlaylist(
                    id = playlist.id,
                    title = playlist.title,
                    author = playlist.author?.name,
                    songCountText = playlist.songCountText,
                    thumbnailUrl = playlist.thumbnail
                )
            }
    }

    /**
     * Get songs from a playlist or album.
     */
    suspend fun getPlaylistSongs(playlistId: String): Result<List<WearSong>> = runCatching {
        val playlistPage = YouTube.playlist(playlistId).getOrThrow()
        playlistPage.songs.map { song ->
            WearSong(
                id = song.id,
                title = song.title,
                artist = song.artists.joinToString { it.name },
                thumbnailUrl = song.thumbnail,
                duration = song.duration
            )
        }
    }

    /**
     * Get top songs from an artist.
     */
    suspend fun getArtistSongs(artistId: String): Result<List<WearSong>> = runCatching {
        val artistPage = YouTube.artist(artistId).getOrThrow()
        artistPage.sections
            .flatMap { section -> section.items.filterIsInstance<SongItem>() }
            .take(20)
            .map { song ->
                WearSong(
                    id = song.id,
                    title = song.title,
                    artist = song.artists.joinToString { it.name },
                    thumbnailUrl = song.thumbnail,
                    duration = song.duration
                )
            }
    }
}
