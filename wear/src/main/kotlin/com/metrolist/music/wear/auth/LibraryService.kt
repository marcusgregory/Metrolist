package com.metrolist.music.wear.auth

import com.metrolist.music.wear.presentation.search.WearSong
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for fetching user's library content (liked songs, playlists) using TV client.
 * OAuth tokens from Device Code Flow require TV client for authenticated endpoints.
 */
@Singleton
class LibraryService @Inject constructor(
    private val httpClient: HttpClient,
    private val tokenManager: TokenManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val BROWSE_ID_LIKED_SONGS = "FEmusic_liked_videos"
        // Use FEmusic_liked_playlists instead of FEmusic_library_landing for TV client
        const val BROWSE_ID_LIBRARY_PLAYLISTS = "FEmusic_liked_playlists"
    }

    /**
     * Fetch user's liked songs.
     */
    suspend fun getLikedSongs(): Result<List<WearSong>> = runCatching {
        val accessToken = tokenManager.getValidToken()
            ?: throw Exception("No valid token available")

        val requestBody = BrowseRequest(
            context = TvClientContext(
                client = TvClientContext.Client(
                    clientName = "TVHTML5",
                    clientVersion = "7.20260124.00.00"
                )
            ),
            browseId = BROWSE_ID_LIKED_SONGS
        )

        val response = httpClient.post("https://www.youtube.com/youtubei/v1/browse") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", "7")
            header("X-YouTube-Client-Version", "7.20260124.00.00")
            setBody(json.encodeToString(BrowseRequest.serializer(), requestBody))
        }

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Liked songs response length: ${body.length}")
            parseLikedSongsResponse(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Liked songs request failed: ${response.status} - $errorBody")
            throw Exception("Failed to get liked songs: ${response.status}")
        }
    }

    /**
     * Fetch user's playlists.
     */
    suspend fun getUserPlaylists(): Result<List<LibraryPlaylist>> = runCatching {
        val accessToken = tokenManager.getValidToken()
            ?: throw Exception("No valid token available")

        val requestBody = BrowseRequest(
            context = TvClientContext(
                client = TvClientContext.Client(
                    clientName = "TVHTML5",
                    clientVersion = "7.20260124.00.00"
                )
            ),
            browseId = BROWSE_ID_LIBRARY_PLAYLISTS
        )

        val response = httpClient.post("https://www.youtube.com/youtubei/v1/browse") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", "7")
            header("X-YouTube-Client-Version", "7.20260124.00.00")
            setBody(json.encodeToString(BrowseRequest.serializer(), requestBody))
        }

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Playlists response length: ${body.length}")
            parsePlaylistsResponse(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Playlists request failed: ${response.status} - $errorBody")
            throw Exception("Failed to get playlists: ${response.status}")
        }
    }

    /**
     * Parse the liked songs response from YouTube TV client.
     */
    private fun parseLikedSongsResponse(responseBody: String): List<WearSong> {
        val songs = mutableListOf<WearSong>()

        try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val contents = jsonElement.jsonObject["contents"]?.jsonObject

            // Log the top-level keys to understand the structure
            Timber.d("Response top-level keys: ${jsonElement.jsonObject.keys}")
            Timber.d("Contents keys: ${contents?.keys}")

            // Try different paths for TV client response
            // Path 1: Standard music browse response
            val sectionList = contents?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            // Path 2: TV client uses tvBrowseRenderer
            val tvBrowse = contents?.get("tvBrowseRenderer")?.jsonObject
            if (tvBrowse != null) {
                Timber.d("Found tvBrowseRenderer, keys: ${tvBrowse.keys}")
                val tvContent = tvBrowse["content"]?.jsonObject
                tvContent?.keys?.let { Timber.d("tvBrowseRenderer content keys: $it") }

                // Navigate TV structure: tvSecondaryNavRenderer -> sections
                val tvSecondaryNav = tvContent?.get("tvSecondaryNavRenderer")?.jsonObject
                if (tvSecondaryNav != null) {
                    Timber.d("tvSecondaryNavRenderer keys: ${tvSecondaryNav.keys}")

                    // Try to find sections/content
                    val sections = tvSecondaryNav["sections"]?.jsonArray
                    sections?.forEachIndexed { index, section ->
                        if (index < 3) Timber.d("TV Section $index keys: ${section.jsonObject.keys}")
                        parseTvSection(section, songs)
                    }

                    // Also try "content" inside tvSecondaryNavRenderer
                    val navContent = tvSecondaryNav["content"]?.jsonObject
                    if (navContent != null) {
                        Timber.d("tvSecondaryNavRenderer content keys: ${navContent.keys}")
                        parseTvContent(navContent, songs)
                    }
                }
            }

            // Path 3: Try twoColumnBrowseResultsRenderer (sometimes used)
            val twoColumn = contents?.get("twoColumnBrowseResultsRenderer")?.jsonObject
            if (twoColumn != null) {
                Timber.d("Found twoColumnBrowseResultsRenderer")
            }

            // Path 4: Direct sectionListRenderer
            val directSectionList = contents?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray
            if (directSectionList != null) {
                Timber.d("Found direct sectionListRenderer with ${directSectionList.size} sections")
                directSectionList.forEach { section ->
                    Timber.d("Section keys: ${section.jsonObject.keys}")
                    parseSection(section, songs)
                }
            }

            sectionList?.forEach { section ->
                Timber.d("Section keys: ${section.jsonObject.keys}")
                parseSection(section, songs)
            }

            Timber.d("Parsed ${songs.size} liked songs")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing liked songs response")
        }

        return songs
    }

    private fun parseSection(section: JsonElement, songs: MutableList<WearSong>) {
        val musicShelf = section.jsonObject["musicShelfRenderer"]?.jsonObject
            ?: section.jsonObject["musicPlaylistShelfRenderer"]?.jsonObject
            ?: section.jsonObject["playlistVideoListRenderer"]?.jsonObject
            ?: section.jsonObject["itemSectionRenderer"]?.jsonObject

        if (musicShelf != null) {
            Timber.d("Found shelf renderer, keys: ${musicShelf.keys}")
        }

        // Try contents array
        val contentsArray = musicShelf?.get("contents")?.jsonArray
            ?: section.jsonObject["contents"]?.jsonArray

        contentsArray?.take(5)?.forEachIndexed { index, item ->
            Timber.d("Item $index keys: ${item.jsonObject.keys}")
        }

        contentsArray?.forEach { item ->
            parseSongItem(item)?.let { songs.add(it) }
        }
    }

    private fun parseTvSection(section: JsonElement, songs: MutableList<WearSong>) {
        try {
            val sectionObj = section.jsonObject
            // TV sections might have different renderers
            val sectionRenderer = sectionObj["tvSecondaryNavSectionRenderer"]?.jsonObject
                ?: sectionObj["sectionRenderer"]?.jsonObject
                ?: sectionObj

            Timber.d("TV section renderer keys: ${sectionRenderer.keys}")

            // Look for tabs or content
            val tabs = sectionRenderer["tabs"]?.jsonArray
            tabs?.forEach { tab ->
                val tabRenderer = tab.jsonObject["tabRenderer"]?.jsonObject
                    ?: tab.jsonObject["tvSecondaryNavSectionTabRenderer"]?.jsonObject
                if (tabRenderer != null) {
                    Timber.d("Tab renderer keys: ${tabRenderer.keys}")
                    val tabContent = tabRenderer["content"]?.jsonObject
                    if (tabContent != null) {
                        parseTvContent(tabContent, songs)
                    }
                }
            }

            // Also check for direct content
            val content = sectionRenderer["content"]?.jsonObject
            if (content != null) {
                parseTvContent(content, songs)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing TV section")
        }
    }

    private fun parseTvContent(content: JsonElement, songs: MutableList<WearSong>) {
        try {
            val contentObj = content.jsonObject
            Timber.d("parseTvContent keys: ${contentObj.keys}")

            // Try various TV content structures
            val tvSurfaceContentRenderer = contentObj["tvSurfaceContentRenderer"]?.jsonObject
            if (tvSurfaceContentRenderer != null) {
                Timber.d("tvSurfaceContentRenderer keys: ${tvSurfaceContentRenderer.keys}")
                val surfaceContent = tvSurfaceContentRenderer["content"]?.jsonObject
                if (surfaceContent != null) {
                    parseTvContent(surfaceContent, songs)
                }
            }

            // Try sectionListRenderer inside TV content
            val sectionListRenderer = contentObj["sectionListRenderer"]?.jsonObject
            if (sectionListRenderer != null) {
                val sections = sectionListRenderer["contents"]?.jsonArray
                sections?.forEach { section ->
                    parseSection(section, songs)
                }
            }

            // Try gridRenderer for TV content (uses tileRenderer)
            val gridRenderer = contentObj["gridRenderer"]?.jsonObject
            if (gridRenderer != null) {
                Timber.d("gridRenderer keys: ${gridRenderer.keys}")
                val items = gridRenderer["items"]?.jsonArray
                items?.take(3)?.forEachIndexed { index, item ->
                    Timber.d("Grid item $index keys: ${item.jsonObject.keys}")
                }
                items?.forEach { item ->
                    // TV client uses tileRenderer
                    val tileRenderer = item.jsonObject["tileRenderer"]?.jsonObject
                    if (tileRenderer != null) {
                        parseTileRenderer(tileRenderer)?.let { songs.add(it) }
                    } else {
                        parseSongItem(item)?.let { songs.add(it) }
                    }
                }
            }

            // Try musicShelfRenderer
            val musicShelfRenderer = contentObj["musicShelfRenderer"]?.jsonObject
            if (musicShelfRenderer != null) {
                Timber.d("musicShelfRenderer in TV content, keys: ${musicShelfRenderer.keys}")
                val contents = musicShelfRenderer["contents"]?.jsonArray
                contents?.forEach { item ->
                    parseSongItem(item)?.let { songs.add(it) }
                }
            }

            // Try playlistVideoListRenderer (common for liked videos)
            val playlistVideoListRenderer = contentObj["playlistVideoListRenderer"]?.jsonObject
            if (playlistVideoListRenderer != null) {
                Timber.d("playlistVideoListRenderer keys: ${playlistVideoListRenderer.keys}")
                val contents = playlistVideoListRenderer["contents"]?.jsonArray
                contents?.take(3)?.forEachIndexed { index, item ->
                    Timber.d("Playlist video item $index keys: ${item.jsonObject.keys}")
                }
                contents?.forEach { item ->
                    parseTvVideoItem(item)?.let { songs.add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing TV content")
        }
    }

    private fun parseTileRenderer(tileRenderer: JsonObject): WearSong? {
        try {
            // Get videoId from onSelectCommand or navigationEndpoint
            val videoId = tileRenderer["onSelectCommand"]?.jsonObject
                ?.get("watchEndpoint")?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: tileRenderer["onFocusCommand"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: tileRenderer["header"]?.jsonObject
                    ?.get("tileHeaderRenderer")?.jsonObject
                    ?.get("onSelectCommand")?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull

            if (videoId == null) {
                return null
            }

            // Get metadata from header
            val header = tileRenderer["header"]?.jsonObject?.get("tileHeaderRenderer")?.jsonObject
            val metadata = tileRenderer["metadata"]?.jsonObject?.get("tileMetadataRenderer")?.jsonObject

            // Log full structure for first tile only (to debug)
            if (tileRenderer.keys.contains("metadata")) {
                val metadataObj = tileRenderer["metadata"]?.jsonObject
                Timber.d("TILE metadata keys: ${metadataObj?.keys}")
                val tileMetadataRenderer = metadataObj?.get("tileMetadataRenderer")?.jsonObject
                Timber.d("TILE tileMetadataRenderer keys: ${tileMetadataRenderer?.keys}")

                // Log lines structure
                val lines = tileMetadataRenderer?.get("lines")?.jsonArray
                Timber.d("TILE lines count: ${lines?.size}")
                lines?.forEachIndexed { idx, line ->
                    Timber.d("TILE line $idx: $line")
                }

                // Log title structure in metadata
                val metaTitle = tileMetadataRenderer?.get("title")
                Timber.d("TILE metadata title: $metaTitle")
            }

            // Log header structure
            if (header != null) {
                Timber.d("TILE header keys: ${header.keys}")
                val headerTitle = header["title"]
                Timber.d("TILE header title: $headerTitle")
            }

            // Get title - try multiple paths
            var title = "Unknown"

            // Path 1: header.title
            header?.get("title")?.jsonObject?.let { titleObj ->
                title = titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: title
            }

            // Path 2: metadata.title
            if (title == "Unknown") {
                metadata?.get("title")?.jsonObject?.let { titleObj ->
                    title = titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                        ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: title
                }
            }

            // Get artist - try multiple paths
            var artist = "Unknown Artist"

            // Path 1: metadata.lines (TV client structure)
            val lines = metadata?.get("lines")?.jsonArray
            lines?.forEach { line ->
                if (artist != "Unknown Artist") return@forEach
                val lineRenderer = line.jsonObject["lineRenderer"]?.jsonObject
                val items = lineRenderer?.get("items")?.jsonArray
                items?.forEach { lineItem ->
                    if (artist != "Unknown Artist") return@forEach
                    val lineItemRenderer = lineItem.jsonObject["lineItemRenderer"]?.jsonObject
                    val text = lineItemRenderer?.get("text")?.jsonObject

                    // Try simpleText first
                    text?.get("simpleText")?.jsonPrimitive?.contentOrNull?.let { simpleText ->
                        if (simpleText != " • " && simpleText != " · " && simpleText != "•" && simpleText != "·") {
                            artist = simpleText
                        }
                    }

                    // Try runs
                    if (artist == "Unknown Artist") {
                        text?.get("runs")?.jsonArray?.forEach { run ->
                            val runText = run.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                            if (runText != null && runText != " • " && runText != " · " && runText != "•" && runText != "·" && artist == "Unknown Artist") {
                                artist = runText
                            }
                        }
                    }
                }
            }

            // Path 2: header.subtitle
            if (artist == "Unknown Artist") {
                header?.get("subtitle")?.jsonObject?.let { subtitle ->
                    artist = subtitle["simpleText"]?.jsonPrimitive?.contentOrNull
                        ?: subtitle["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: artist
                }
            }

            // Path 3: contentDetails or byline
            if (artist == "Unknown Artist") {
                tileRenderer["contentDetails"]?.jsonObject?.let { details ->
                    Timber.d("TILE contentDetails: $details")
                }
                tileRenderer["byline"]?.jsonObject?.let { byline ->
                    artist = byline["simpleText"]?.jsonPrimitive?.contentOrNull
                        ?: byline["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: artist
                }
            }

            // Get thumbnail - prefer standard YouTube thumbnail URL for proper aspect ratio
            // TV client returns square thumbnails, but we want 16:9 for the player
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            Timber.d("Parsed tile: $title by $artist (videoId: $videoId)")

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = null
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing tileRenderer")
            return null
        }
    }

    private fun parseTvVideoItem(item: JsonElement): WearSong? {
        try {
            val renderer = item.jsonObject["playlistVideoRenderer"]?.jsonObject
                ?: item.jsonObject["tvMusicVideoRenderer"]?.jsonObject
                ?: item.jsonObject["gridVideoRenderer"]?.jsonObject
                ?: return null

            Timber.d("TV video renderer keys: ${renderer.keys}")

            val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val title = renderer["title"]?.let { titleElement ->
                when {
                    titleElement.jsonObject.containsKey("simpleText") ->
                        titleElement.jsonObject["simpleText"]?.jsonPrimitive?.contentOrNull
                    titleElement.jsonObject.containsKey("runs") ->
                        titleElement.jsonObject["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            } ?: "Unknown"

            val artist = renderer["shortBylineText"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: renderer["ownerText"]?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Artist"

            // Use standard YouTube thumbnail URL for proper 16:9 aspect ratio
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            val durationText = renderer["lengthText"]?.jsonObject
                ?.get("simpleText")?.jsonPrimitive?.contentOrNull

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = durationText?.let { parseTime(it) }
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing TV video item")
            return null
        }
    }

    /**
     * Parse a song item from the response.
     */
    private fun parseSongItem(item: JsonElement): WearSong? {
        try {
            val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                ?: item.jsonObject["playlistPanelVideoRenderer"]?.jsonObject
                ?: return null

            // Try to get video ID from playlistItemData or navigationEndpoint
            val videoId = renderer["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            // Get title
            val title = renderer["flexColumns"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: renderer["title"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown"

            // Get artist
            val artist = renderer["flexColumns"]?.jsonArray?.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.filterNot { it == " • " || it == " & " }
                ?.joinToString(", ")
                ?: renderer["shortBylineText"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Artist"

            // Use standard YouTube thumbnail URL for proper 16:9 aspect ratio
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            // Get duration
            val durationText = renderer["fixedColumns"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: renderer["lengthText"]?.jsonObject?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull

            val duration = durationText?.let { parseTime(it) }

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = duration
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing song item")
            return null
        }
    }

    /**
     * Parse playlists response.
     */
    private fun parsePlaylistsResponse(responseBody: String): List<LibraryPlaylist> {
        val playlists = mutableListOf<LibraryPlaylist>()

        try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val contents = jsonElement.jsonObject["contents"]?.jsonObject

            Timber.d("Playlists contents keys: ${contents?.keys}")

            // Standard path
            val sectionList = contents?.get("singleColumnBrowseResultsRenderer")?.jsonObject
                ?.get("tabs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("tabRenderer")?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("sectionListRenderer")?.jsonObject
                ?.get("contents")?.jsonArray

            // TV client path
            val tvBrowse = contents?.get("tvBrowseRenderer")?.jsonObject
            if (tvBrowse != null) {
                Timber.d("Playlists: Found tvBrowseRenderer")
                val tvContent = tvBrowse["content"]?.jsonObject
                val tvSecondaryNav = tvContent?.get("tvSecondaryNavRenderer")?.jsonObject
                if (tvSecondaryNav != null) {
                    Timber.d("Playlists: tvSecondaryNavRenderer keys: ${tvSecondaryNav.keys}")
                    val sections = tvSecondaryNav["sections"]?.jsonArray
                    sections?.forEach { section ->
                        parseTvPlaylistSection(section, playlists)
                    }
                }
            }

            sectionList?.forEach { section ->
                val gridRenderer = section.jsonObject["gridRenderer"]?.jsonObject
                    ?: section.jsonObject["musicShelfRenderer"]?.jsonObject

                gridRenderer?.get("items")?.jsonArray?.forEach { item ->
                    parsePlaylistItem(item)?.let { playlists.add(it) }
                }

                gridRenderer?.get("contents")?.jsonArray?.forEach { item ->
                    parsePlaylistItem(item)?.let { playlists.add(it) }
                }
            }

            Timber.d("Parsed ${playlists.size} playlists")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing playlists response")
        }

        return playlists
    }

    private fun parseTvPlaylistSection(section: JsonElement, playlists: MutableList<LibraryPlaylist>) {
        try {
            val sectionRenderer = section.jsonObject["tvSecondaryNavSectionRenderer"]?.jsonObject
                ?: section.jsonObject

            val tabs = sectionRenderer["tabs"]?.jsonArray
            tabs?.forEach { tab ->
                val tabRenderer = tab.jsonObject["tabRenderer"]?.jsonObject
                val tabContent = tabRenderer?.get("content")?.jsonObject
                if (tabContent != null) {
                    parseTvPlaylistContent(tabContent, playlists)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing TV playlist section")
        }
    }

    private fun parseTvPlaylistContent(content: JsonElement, playlists: MutableList<LibraryPlaylist>) {
        try {
            val contentObj = content.jsonObject
            Timber.d("parseTvPlaylistContent keys: ${contentObj.keys}")

            val tvSurfaceContent = contentObj["tvSurfaceContentRenderer"]?.jsonObject
            if (tvSurfaceContent != null) {
                val innerContent = tvSurfaceContent["content"]?.jsonObject
                if (innerContent != null) {
                    parseTvPlaylistContent(innerContent, playlists)
                }
            }

            val gridRenderer = contentObj["gridRenderer"]?.jsonObject
            if (gridRenderer != null) {
                Timber.d("Playlists gridRenderer found, keys: ${gridRenderer.keys}")
                val items = gridRenderer["items"]?.jsonArray
                items?.take(3)?.forEachIndexed { index, item ->
                    Timber.d("Playlist grid item $index keys: ${item.jsonObject.keys}")
                }
                items?.forEach { item ->
                    val tileRenderer = item.jsonObject["tileRenderer"]?.jsonObject
                    if (tileRenderer != null) {
                        parseTileAsPlaylist(tileRenderer)?.let { playlists.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing TV playlist content")
        }
    }

    private fun parseTileAsPlaylist(tileRenderer: JsonObject): LibraryPlaylist? {
        try {
            // Get playlist ID from navigation endpoint
            val playlistId = tileRenderer["onSelectCommand"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
                ?.removePrefix("VL")
                ?: return null

            val header = tileRenderer["header"]?.jsonObject?.get("tileHeaderRenderer")?.jsonObject
            val metadata = tileRenderer["metadata"]?.jsonObject?.get("tileMetadataRenderer")?.jsonObject

            // Log full structure for debugging
            Timber.d("PLAYLIST tile keys: ${tileRenderer.keys}")
            Timber.d("PLAYLIST header: $header")
            Timber.d("PLAYLIST metadata keys: ${metadata?.keys}")

            // Try to get title from multiple locations
            var title = "Unknown Playlist"

            // Path 1: header.title
            header?.get("title")?.let { titleElement ->
                Timber.d("PLAYLIST header.title: $titleElement")
                val titleObj = titleElement.jsonObject
                title = titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: title
            }

            // Path 2: metadata.title
            if (title == "Unknown Playlist") {
                metadata?.get("title")?.let { titleElement ->
                    Timber.d("PLAYLIST metadata.title: $titleElement")
                    val titleObj = titleElement.jsonObject
                    title = titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                        ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                            ?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: title
                }
            }

            // Path 3: Direct title in tileRenderer
            if (title == "Unknown Playlist") {
                tileRenderer["title"]?.let { titleElement ->
                    Timber.d("PLAYLIST direct title: $titleElement")
                    try {
                        val titleObj = titleElement.jsonObject
                        title = titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                            ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                                ?.get("text")?.jsonPrimitive?.contentOrNull
                            ?: title
                    } catch (e: Exception) {
                        // Might be a primitive
                        title = titleElement.jsonPrimitive.contentOrNull ?: title
                    }
                }
            }

            // Get subtitle from lines
            val lines = metadata?.get("lines")?.jsonArray
            var subtitle: String? = null

            Timber.d("PLAYLIST lines: $lines")

            lines?.forEach { line ->
                val lineRenderer = line.jsonObject["lineRenderer"]?.jsonObject
                val items = lineRenderer?.get("items")?.jsonArray
                items?.forEach { lineItem ->
                    val lineItemRenderer = lineItem.jsonObject["lineItemRenderer"]?.jsonObject
                    val text = lineItemRenderer?.get("text")?.jsonObject
                    val simpleText = text?.get("simpleText")?.jsonPrimitive?.contentOrNull
                    val runText = text?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                    if (subtitle == null && (simpleText != null || runText != null)) {
                        subtitle = simpleText ?: runText
                    }
                }
            }

            // Get thumbnail - try multiple paths
            var thumbnailUrl = ""

            // Path 1: contentImage.singleHeroImageRenderer
            tileRenderer["contentImage"]?.jsonObject
                ?.get("singleHeroImageRenderer")?.jsonObject
                ?.get("image")?.jsonObject
                ?.get("sources")?.jsonArray?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull?.let {
                    thumbnailUrl = it
                }

            // Path 2: header.thumbnail.thumbnails (from logs this is where it is)
            if (thumbnailUrl.isEmpty()) {
                header?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.contentOrNull?.let {
                        thumbnailUrl = it
                    }
            }

            Timber.d("Parsed playlist: $title (id: $playlistId, thumbnail: ${thumbnailUrl.take(50)}...)")

            return LibraryPlaylist(
                id = playlistId,
                title = title,
                subtitle = subtitle,
                thumbnailUrl = thumbnailUrl
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing tile as playlist")
            return null
        }
    }

    /**
     * Parse a playlist item.
     */
    private fun parsePlaylistItem(item: JsonElement): LibraryPlaylist? {
        try {
            val renderer = item.jsonObject["musicTwoRowItemRenderer"]?.jsonObject
                ?: item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                ?: return null

            // Get playlist ID
            val browseId = renderer["navigationEndpoint"]?.jsonObject
                ?.get("browseEndpoint")?.jsonObject
                ?.get("browseId")?.jsonPrimitive?.contentOrNull
                ?: return null

            // Skip if it's not a playlist (e.g., "New playlist" button)
            if (!browseId.startsWith("VL") && !browseId.startsWith("PL")) {
                return null
            }

            val playlistId = browseId.removePrefix("VL")

            // Get title
            val title = renderer["title"]?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: renderer["flexColumns"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject
                    ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Playlist"

            // Get subtitle (song count)
            val subtitle = renderer["subtitle"]?.jsonObject
                ?.get("runs")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
                ?: renderer["flexColumns"]?.jsonArray?.getOrNull(1)?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject
                    ?.get("runs")?.jsonArray
                    ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                    ?.joinToString("")

            // Get thumbnail
            val thumbnails = renderer["thumbnailRenderer"]?.jsonObject
                ?.get("musicThumbnailRenderer")?.jsonObject
                ?.get("thumbnail")?.jsonObject
                ?.get("thumbnails")?.jsonArray
                ?: renderer["thumbnail"]?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject
                    ?.get("thumbnails")?.jsonArray

            val thumbnailUrl = thumbnails?.lastOrNull()?.jsonObject
                ?.get("url")?.jsonPrimitive?.contentOrNull ?: ""

            return LibraryPlaylist(
                id = playlistId,
                title = title,
                subtitle = subtitle,
                thumbnailUrl = thumbnailUrl
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing playlist item")
            return null
        }
    }

    /**
     * Fetch songs from a specific playlist using TV client.
     */
    suspend fun getPlaylistSongs(playlistId: String): Result<List<WearSong>> = runCatching {
        val accessToken = tokenManager.getValidToken()
            ?: throw Exception("No valid token available")

        // For Liked Songs (LM), use the liked songs browse ID
        val browseId = if (playlistId == "LM") {
            BROWSE_ID_LIKED_SONGS
        } else {
            "VL$playlistId"
        }

        Timber.d("Fetching playlist songs for: $browseId")

        val requestBody = BrowseRequest(
            context = TvClientContext(
                client = TvClientContext.Client(
                    clientName = "TVHTML5",
                    clientVersion = "7.20260124.00.00"
                )
            ),
            browseId = browseId
        )

        val response = httpClient.post("https://www.youtube.com/youtubei/v1/browse") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $accessToken")
            header("X-Goog-Api-Format-Version", "1")
            header("X-YouTube-Client-Name", "7")
            header("X-YouTube-Client-Version", "7.20260124.00.00")
            setBody(json.encodeToString(BrowseRequest.serializer(), requestBody))
        }

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Playlist songs response length: ${body.length}")
            parsePlaylistSongsResponse(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Playlist songs request failed: ${response.status} - $errorBody")
            throw Exception("Failed to get playlist songs: ${response.status}")
        }
    }

    /**
     * Parse playlist songs response from TV client.
     */
    private fun parsePlaylistSongsResponse(responseBody: String): List<WearSong> {
        val songs = mutableListOf<WearSong>()

        try {
            val jsonElement = json.parseToJsonElement(responseBody)
            val root = jsonElement.jsonObject
            val contents = root["contents"]?.jsonObject

            Timber.d("Playlist response root keys: ${root.keys}")
            Timber.d("Playlist contents keys: ${contents?.keys}")

            // TV client path 1: tvBrowseRenderer.content.tvSecondaryNavRenderer
            val tvBrowse = contents?.get("tvBrowseRenderer")?.jsonObject
            if (tvBrowse != null) {
                Timber.d("tvBrowseRenderer keys: ${tvBrowse.keys}")
                val tvContent = tvBrowse["content"]?.jsonObject
                Timber.d("tvBrowseRenderer.content keys: ${tvContent?.keys}")

                val tvSecondaryNav = tvContent?.get("tvSecondaryNavRenderer")?.jsonObject
                if (tvSecondaryNav != null) {
                    Timber.d("Found tvSecondaryNavRenderer")
                    val sections = tvSecondaryNav["sections"]?.jsonArray
                    sections?.forEach { section ->
                        parseTvSection(section, songs)
                    }
                }

                // TV client path 2: tvBrowseRenderer.content.tvSurfaceContentRenderer
                val tvSurfaceContent = tvContent?.get("tvSurfaceContentRenderer")?.jsonObject
                if (tvSurfaceContent != null && songs.isEmpty()) {
                    Timber.d("Found tvSurfaceContentRenderer, keys: ${tvSurfaceContent.keys}")
                    val content = tvSurfaceContent["content"]?.jsonObject
                    Timber.d("tvSurfaceContentRenderer.content keys: ${content?.keys}")

                    // Try twoColumnRenderer (playlist detail view)
                    val twoColumnRenderer = content?.get("twoColumnRenderer")?.jsonObject
                    if (twoColumnRenderer != null) {
                        Timber.d("twoColumnRenderer keys: ${twoColumnRenderer.keys}")
                        parseTwoColumnPlaylist(twoColumnRenderer, songs)
                    }

                    // Try sectionListRenderer
                    val sectionList = content?.get("sectionListRenderer")?.jsonObject
                    if (sectionList != null && songs.isEmpty()) {
                        Timber.d("sectionListRenderer keys: ${sectionList.keys}")
                        val sectionContents = sectionList["contents"]?.jsonArray
                        sectionContents?.forEach { section ->
                            parseTvPlaylistSongsSection(section, songs)
                        }
                    }

                    // Try gridRenderer
                    val gridRenderer = content?.get("gridRenderer")?.jsonObject
                    if (gridRenderer != null && songs.isEmpty()) {
                        Timber.d("gridRenderer keys: ${gridRenderer.keys}")
                        val items = gridRenderer["items"]?.jsonArray
                        items?.forEach { item ->
                            parseTileRenderer(item.jsonObject["tileRenderer"]?.jsonObject ?: return@forEach)?.let { song ->
                                songs.add(song)
                            }
                        }
                    }
                }
            }

            Timber.d("Parsed ${songs.size} playlist songs")
        } catch (e: Exception) {
            Timber.e(e, "Error parsing playlist songs response")
        }

        return songs
    }

    /**
     * Parse twoColumnRenderer for playlist songs (TV client playlist detail view).
     */
    private fun parseTwoColumnPlaylist(twoColumnRenderer: JsonObject, songs: MutableList<WearSong>) {
        try {
            // TV client uses leftColumn/rightColumn, not primaryContents/secondaryContents
            val leftColumn = twoColumnRenderer["leftColumn"]?.jsonObject
            Timber.d("twoColumnRenderer.leftColumn keys: ${leftColumn?.keys}")

            // Parse left column content
            if (leftColumn != null) {
                parseColumnContent(leftColumn, songs)
            }

            // Also check right column if left column didn't have songs
            if (songs.isEmpty()) {
                val rightColumn = twoColumnRenderer["rightColumn"]?.jsonObject
                Timber.d("twoColumnRenderer.rightColumn keys: ${rightColumn?.keys}")
                if (rightColumn != null) {
                    parseColumnContent(rightColumn, songs)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing twoColumnRenderer")
        }
    }

    /**
     * Parse column content from twoColumnRenderer.
     */
    private fun parseColumnContent(column: JsonObject, songs: MutableList<WearSong>) {
        try {
            Timber.d("Parsing column with keys: ${column.keys}")

            // Try tvSecondaryNavRenderer (most common for playlists)
            column["tvSecondaryNavRenderer"]?.jsonObject?.let { tvSecondaryNav ->
                Timber.d("Found tvSecondaryNavRenderer in column, keys: ${tvSecondaryNav.keys}")
                val sections = tvSecondaryNav["sections"]?.jsonArray
                sections?.forEach { section ->
                    parseTvSection(section, songs)
                }
            }

            // Try sectionListRenderer
            column["sectionListRenderer"]?.jsonObject?.let { sectionList ->
                Timber.d("Found sectionListRenderer in column, keys: ${sectionList.keys}")
                val contents = sectionList["contents"]?.jsonArray
                contents?.forEach { section ->
                    val sectionObj = section.jsonObject
                    Timber.d("sectionList section keys: ${sectionObj.keys}")

                    // Try gridRenderer
                    sectionObj["gridRenderer"]?.jsonObject?.let { gridRenderer ->
                        Timber.d("Found gridRenderer in column, keys: ${gridRenderer.keys}")
                        val items = gridRenderer["items"]?.jsonArray
                        Timber.d("gridRenderer items count: ${items?.size}")
                        items?.forEachIndexed { index, item ->
                            val itemObj = item.jsonObject
                            if (index < 3) {
                                Timber.d("gridRenderer item $index keys: ${itemObj.keys}")
                            }
                            itemObj["tileRenderer"]?.jsonObject?.let { tileRenderer ->
                                parseTileRenderer(tileRenderer)?.let { song ->
                                    songs.add(song)
                                }
                            }
                        }
                    }

                    // Try itemSectionRenderer
                    sectionObj["itemSectionRenderer"]?.jsonObject?.let { itemSection ->
                        Timber.d("Found itemSectionRenderer in column")
                        val sectionContents = itemSection["contents"]?.jsonArray
                        sectionContents?.forEach { item ->
                            val itemObj = item.jsonObject
                            itemObj["playlistVideoRenderer"]?.jsonObject?.let { renderer ->
                                parsePlaylistVideoRenderer(renderer)?.let { songs.add(it) }
                            }
                        }
                    }
                }
            }

            // Try gridRenderer directly
            column["gridRenderer"]?.jsonObject?.let { gridRenderer ->
                Timber.d("Found gridRenderer directly in column, keys: ${gridRenderer.keys}")
                val items = gridRenderer["items"]?.jsonArray
                Timber.d("gridRenderer items count: ${items?.size}")
                items?.forEach { item ->
                    val itemObj = item.jsonObject
                    itemObj["tileRenderer"]?.jsonObject?.let { tileRenderer ->
                        parseTileRenderer(tileRenderer)?.let { song ->
                            songs.add(song)
                        }
                    }
                }
            }

            // Try playlistVideoListRenderer (TV client playlist songs)
            column["playlistVideoListRenderer"]?.jsonObject?.let { playlistVideoList ->
                Timber.d("Found playlistVideoListRenderer, keys: ${playlistVideoList.keys}")
                val contents = playlistVideoList["contents"]?.jsonArray
                Timber.d("playlistVideoListRenderer contents count: ${contents?.size}")
                contents?.forEachIndexed { index, item ->
                    val itemObj = item.jsonObject
                    if (index < 3) {
                        Timber.d("playlistVideoListRenderer item $index keys: ${itemObj.keys}")
                    }

                    // Try tileRenderer (TV client uses tiles for playlist songs)
                    itemObj["tileRenderer"]?.jsonObject?.let { tileRenderer ->
                        parseTileRenderer(tileRenderer)?.let { song ->
                            songs.add(song)
                        }
                    }

                    // Try playlistVideoRenderer
                    itemObj["playlistVideoRenderer"]?.jsonObject?.let { renderer ->
                        parsePlaylistVideoRenderer(renderer)?.let { song ->
                            songs.add(song)
                        }
                    }

                    // Try playlistPanelVideoRenderer (alternative format)
                    itemObj["playlistPanelVideoRenderer"]?.jsonObject?.let { renderer ->
                        parsePlaylistPanelVideoRenderer(renderer)?.let { song ->
                            songs.add(song)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing column content")
        }
    }

    /**
     * Parse playlistPanelVideoRenderer for song info (TV client format).
     */
    private fun parsePlaylistPanelVideoRenderer(renderer: JsonObject): WearSong? {
        try {
            Timber.d("playlistPanelVideoRenderer keys: ${renderer.keys}")

            val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull
                ?: renderer["navigationEndpoint"]?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val title = renderer["title"]?.jsonObject?.let { titleObj ->
                titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: "Unknown"

            val artist = renderer["shortBylineText"]?.jsonObject?.let { byline ->
                byline["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: byline["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: renderer["longBylineText"]?.jsonObject?.let { byline ->
                byline["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: byline["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: "Unknown Artist"

            // Use standard YouTube thumbnail URL for proper 16:9 aspect ratio
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            val duration = renderer["lengthText"]?.jsonObject?.let { lengthText ->
                val timeStr = lengthText["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: lengthText["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                timeStr?.let { parseTime(it) }
            }

            Timber.d("Parsed song: $title by $artist (id: $videoId)")

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = duration
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing playlistPanelVideoRenderer")
            return null
        }
    }

    /**
     * Parse TV playlist section for songs.
     */
    private fun parseTvPlaylistSongsSection(section: JsonElement, songs: MutableList<WearSong>) {
        try {
            val sectionObj = section.jsonObject
            Timber.d("Playlist section keys: ${sectionObj.keys}")

            // Try musicShelfRenderer
            val musicShelf = sectionObj["musicShelfRenderer"]?.jsonObject
            if (musicShelf != null) {
                Timber.d("Found musicShelfRenderer in playlist")
                val shelfContents = musicShelf["contents"]?.jsonArray
                shelfContents?.forEach { item ->
                    parsePlaylistMusicItem(item)?.let { songs.add(it) }
                }
            }

            // Try itemSectionRenderer
            val itemSection = sectionObj["itemSectionRenderer"]?.jsonObject
            if (itemSection != null) {
                Timber.d("Found itemSectionRenderer, keys: ${itemSection.keys}")
                val sectionContents = itemSection["contents"]?.jsonArray
                sectionContents?.forEach { item ->
                    val itemObj = item.jsonObject
                    Timber.d("itemSectionRenderer item keys: ${itemObj.keys}")

                    // Try playlistVideoRenderer
                    itemObj["playlistVideoRenderer"]?.jsonObject?.let { renderer ->
                        parsePlaylistVideoRenderer(renderer)?.let { songs.add(it) }
                    }

                    // Try musicResponsiveListItemRenderer
                    itemObj["musicResponsiveListItemRenderer"]?.jsonObject?.let { renderer ->
                        parseMusicResponsiveListItem(renderer)?.let { songs.add(it) }
                    }
                }
            }

            // Try gridRenderer
            val gridRenderer = sectionObj["gridRenderer"]?.jsonObject
            if (gridRenderer != null) {
                Timber.d("Found gridRenderer in section")
                val items = gridRenderer["items"]?.jsonArray
                items?.forEach { item ->
                    parseTileRenderer(item.jsonObject["tileRenderer"]?.jsonObject ?: return@forEach)?.let { song ->
                        songs.add(song)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error parsing playlist section")
        }
    }

    /**
     * Parse playlistVideoRenderer for song info.
     */
    private fun parsePlaylistVideoRenderer(renderer: JsonObject): WearSong? {
        try {
            val videoId = renderer["videoId"]?.jsonPrimitive?.contentOrNull ?: return null

            val title = renderer["title"]?.jsonObject?.let { titleObj ->
                titleObj["simpleText"]?.jsonPrimitive?.contentOrNull
                    ?: titleObj["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: "Unknown"

            val artist = renderer["shortBylineText"]?.jsonObject?.let { byline ->
                byline["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: renderer["longBylineText"]?.jsonObject?.let { byline ->
                byline["runs"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.contentOrNull
            } ?: "Unknown Artist"

            // Use standard YouTube thumbnail URL for proper 16:9 aspect ratio
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            val duration = renderer["lengthSeconds"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = duration
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing playlistVideoRenderer")
            return null
        }
    }

    /**
     * Parse musicResponsiveListItemRenderer for song info.
     */
    private fun parseMusicResponsiveListItem(renderer: JsonObject): WearSong? {
        try {
            // Get videoId from playlistItemData or overlay
            val videoId = renderer["playlistItemData"]?.jsonObject
                ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: renderer["overlay"]?.jsonObject
                    ?.get("musicItemThumbnailOverlayRenderer")?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("musicPlayButtonRenderer")?.jsonObject
                    ?.get("playNavigationEndpoint")?.jsonObject
                    ?.get("watchEndpoint")?.jsonObject
                    ?.get("videoId")?.jsonPrimitive?.contentOrNull
                ?: return null

            val flexColumns = renderer["flexColumns"]?.jsonArray

            // Get title from first flex column
            val title = flexColumns?.firstOrNull()?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown"

            // Get artist from second flex column
            val artist = flexColumns?.getOrNull(1)?.jsonObject
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                ?.get("text")?.jsonObject
                ?.get("runs")?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull
                ?: "Unknown Artist"

            // Use standard YouTube thumbnail URL for proper 16:9 aspect ratio
            // Use mqdefault (320x180) for WearOS to save bandwidth
            val thumbnailUrl = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

            return WearSong(
                id = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = null
            )
        } catch (e: Exception) {
            Timber.w(e, "Error parsing musicResponsiveListItemRenderer")
            return null
        }
    }

    /**
     * Parse music item from playlist (musicShelfRenderer contents).
     */
    private fun parsePlaylistMusicItem(item: JsonElement): WearSong? {
        try {
            val itemObj = item.jsonObject
            Timber.d("Playlist music item keys: ${itemObj.keys}")

            // Try musicResponsiveListItemRenderer
            itemObj["musicResponsiveListItemRenderer"]?.jsonObject?.let { renderer ->
                return parseMusicResponsiveListItem(renderer)
            }

            // Try playlistVideoRenderer
            itemObj["playlistVideoRenderer"]?.jsonObject?.let { renderer ->
                return parsePlaylistVideoRenderer(renderer)
            }

            return null
        } catch (e: Exception) {
            Timber.w(e, "Error parsing playlist music item")
            return null
        }
    }

    /**
     * Parse time string (MM:SS or HH:MM:SS) to seconds.
     */
    private fun parseTime(timeString: String): Int? {
        return try {
            val parts = timeString.split(":")
            when (parts.size) {
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Request model
@Serializable
data class BrowseRequest(
    val context: TvClientContext,
    val browseId: String
)

// Result model for playlists
data class LibraryPlaylist(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String
)
