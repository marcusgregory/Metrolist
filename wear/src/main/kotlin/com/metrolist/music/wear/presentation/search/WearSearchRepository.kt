package com.metrolist.music.wear.presentation.search

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for searching songs via YouTube/Innertube API.
 */
@Singleton
class WearSearchRepository @Inject constructor() {

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
}
