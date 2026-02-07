package com.metrolist.music.wear.presentation.search

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.wear.input.RemoteInputIntentHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.music.wear.presentation.SearchIcon

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

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
            is SearchState.Success -> {
                ResultsContent(
                    results = s.results,
                    onSongClick = onSongClick,
                    onNewSearch = { launchRemoteInput(launcher) }
                )
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
    // Auto-launch RemoteInput when screen opens
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
private fun ResultsContent(
    results: List<WearSong>,
    onSongClick: (WearSong) -> Unit,
    onNewSearch: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        item {
            ListHeader {
                Text("${results.size} results")
            }
        }

        item {
            FilledTonalButton(
                onClick = onNewSearch,
                modifier = Modifier.height(32.dp)
            ) {
                Icon(SearchIcon, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("New search")
            }
        }

        items(results, key = { it.id }) { song ->
            SongCard(song = song, onSongClick = onSongClick)
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
                    .clip(CircleShape),
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
