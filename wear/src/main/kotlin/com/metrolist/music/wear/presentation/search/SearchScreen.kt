package com.metrolist.music.wear.presentation.search

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.input.RemoteInputIntentHelper
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.data.model.WearAlbum
import com.metrolist.music.wear.data.model.WearArtist
import com.metrolist.music.wear.data.model.WearPlaylist
import com.metrolist.music.wear.presentation.SearchIcon
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit,
    onAlbumPlay: (WearAlbum, List<WearSong>) -> Unit = { _, _ -> },
    onArtistPlay: (WearArtist, List<WearSong>) -> Unit = { _, _ -> },
    onPlaylistPlay: (WearPlaylist, List<WearSong>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = RemoteInput.getResultsFromIntent(result.data)
            val query = results?.getCharSequence(REMOTE_INPUT_KEY)?.toString()
            if (!query.isNullOrBlank()) {
                viewModel.search(query)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (val s = state) {
            is SearchState.Idle -> {
                IdleContent(launcher = launcher)
            }
            is SearchState.Loading -> {
                CircularProgressIndicator()
            }
            is SearchState.Results -> {
                UnifiedResultsContent(
                    results = s.results,
                    onSongClick = onSongClick,
                    onAlbumClick = { album ->
                        scope.launch {
                            val songs = viewModel.loadAlbumSongs(album.id)
                            if (songs.isNotEmpty()) {
                                onAlbumPlay(album, songs)
                            }
                        }
                    },
                    onArtistClick = { artist ->
                        scope.launch {
                            val songs = viewModel.loadArtistSongs(artist.id)
                            if (songs.isNotEmpty()) {
                                onArtistPlay(artist, songs)
                            }
                        }
                    },
                    onPlaylistClick = { playlist ->
                        scope.launch {
                            val songs = viewModel.loadPlaylistSongs(playlist.id)
                            if (songs.isNotEmpty()) {
                                onPlaylistPlay(playlist, songs)
                            }
                        }
                    },
                    onNewSearch = { launchRemoteInput(launcher) }
                )
            }
            is SearchState.LoadingContent -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Loading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            is SearchState.Error -> {
                ErrorContent(
                    message = s.message,
                    onRetry = viewModel::retry
                )
            }
        }
    }
}

@Composable
private fun IdleContent(launcher: ActivityResultLauncher<Intent>) {
    LaunchedEffect(Unit) {
        launchRemoteInput(launcher)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Metrolist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { launchRemoteInput(launcher) }) {
            Icon(SearchIcon, contentDescription = null)
            Spacer(Modifier.size(4.dp))
            Text("Search")
        }
    }
}

@Composable
private fun UnifiedResultsContent(
    results: UnifiedSearchResults,
    onSongClick: (WearSong) -> Unit,
    onAlbumClick: (WearAlbum) -> Unit,
    onArtistClick: (WearArtist) -> Unit,
    onPlaylistClick: (WearPlaylist) -> Unit,
    onNewSearch: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        // New Search button at top
        item {
            FilledTonalButton(
                onClick = onNewSearch,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(SearchIcon, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("New search")
            }
        }

        // Songs section
        if (results.songs.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Songs")
                }
            }
            items(results.songs, key = { "song_${it.id}" }) { song ->
                SongCard(song = song, onSongClick = onSongClick)
            }
        }

        // Artists section
        if (results.artists.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Artists")
                }
            }
            items(results.artists, key = { "artist_${it.id}" }) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }

        // Albums section
        if (results.albums.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Albums")
                }
            }
            items(results.albums, key = { "album_${it.id}" }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album) }
                )
            }
        }

        // Playlists section
        if (results.playlists.isNotEmpty()) {
            item {
                ListHeader {
                    Text("Playlists")
                }
            }
            items(results.playlists, key = { "playlist_${it.id}" }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }
}

@Composable
private fun SongCard(song: WearSong, onSongClick: (WearSong) -> Unit) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(song.thumbnailUrl)
            .size(64)
            .crossfade(false)
            .memoryCacheKey(song.id)
            .build()
    )

    Button(
        onClick = { onSongClick(song) },
        icon = {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        },
        label = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Text(
                text = song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

private const val REMOTE_INPUT_KEY = "search_query"

private fun launchRemoteInput(launcher: ActivityResultLauncher<Intent>) {
    val remoteInputs = listOf(
        RemoteInput.Builder(REMOTE_INPUT_KEY)
            .setLabel("Search music")
            .build()
    )

    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
    RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)

    launcher.launch(intent)
}
