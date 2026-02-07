package com.metrolist.music.wear.player

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Manages radio queue for auto-play functionality.
 * Fetches related songs when the current queue ends.
 */
class WearRadioQueue(private val videoId: String) {
    private var continuation: String? = null
    private var endpoint: WatchEndpoint? = null
    private var retryCount = 0
    private val maxRetries = 3

    /**
     * Fetches initial radio songs for the given video ID.
     * Uses YouTube Music's radio playlist format (RDAMVM{videoId}).
     */
    suspend fun getInitialStatus(): List<WearMusicService.QueueItem> = withContext(Dispatchers.IO) {
        try {
            // Use radio playlist format
            val radioEndpoint = WatchEndpoint(
                videoId = videoId,
                playlistId = "RDAMVM$videoId",
                params = "wAEB"
            )

            Timber.d("Fetching radio for videoId: $videoId")

            val result = YouTube.next(radioEndpoint).getOrNull()
            if (result != null) {
                endpoint = result.endpoint
                continuation = result.continuation

                val items = result.items
                    .filter { it.id != videoId } // Exclude current song
                    .map { song ->
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artists.joinToString { it.name },
                            artworkUrl = song.thumbnail
                        )
                    }

                Timber.d("Radio loaded ${items.size} songs, has continuation: ${continuation != null}")
                items
            } else {
                Timber.w("Radio fetch returned null, trying fallback")
                fetchFallback()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch radio")
            emptyList()
        }
    }

    /**
     * Fetches the next page of radio songs using continuation token.
     */
    suspend fun nextPage(): List<WearMusicService.QueueItem> = withContext(Dispatchers.IO) {
        val currentContinuation = continuation
        val currentEndpoint = endpoint

        if (currentContinuation == null || currentEndpoint == null) {
            Timber.d("No continuation available for radio")
            return@withContext emptyList()
        }

        if (retryCount >= maxRetries) {
            Timber.w("Max retries reached for radio pagination")
            continuation = null
            return@withContext emptyList()
        }

        try {
            val result = YouTube.next(currentEndpoint, currentContinuation).getOrNull()
            if (result != null) {
                continuation = result.continuation
                retryCount = 0

                result.items.map { song ->
                    WearMusicService.QueueItem(
                        videoId = song.id,
                        title = song.title,
                        artist = song.artists.joinToString { it.name },
                        artworkUrl = song.thumbnail
                    )
                }
            } else {
                retryCount++
                Timber.w("Radio pagination failed, retry $retryCount/$maxRetries")
                emptyList()
            }
        } catch (e: Exception) {
            retryCount++
            Timber.e(e, "Error fetching radio next page")
            emptyList()
        }
    }

    /**
     * Checks if more pages are available.
     */
    fun hasNextPage(): Boolean = continuation != null && retryCount < maxRetries

    /**
     * Fallback: fetch related songs if radio playlist fails.
     */
    private suspend fun fetchFallback(): List<WearMusicService.QueueItem> {
        return try {
            // Try simple next endpoint without radio playlist
            val simpleEndpoint = WatchEndpoint(videoId = videoId)
            val result = YouTube.next(simpleEndpoint).getOrNull()

            if (result != null) {
                endpoint = result.endpoint
                continuation = result.continuation

                result.items
                    .filter { it.id != videoId }
                    .take(10) // Limit fallback results
                    .map { song ->
                        WearMusicService.QueueItem(
                            videoId = song.id,
                            title = song.title,
                            artist = song.artists.joinToString { it.name },
                            artworkUrl = song.thumbnail
                        )
                    }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Fallback also failed")
            emptyList()
        }
    }
}
