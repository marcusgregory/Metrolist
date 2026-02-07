package com.metrolist.music.wear.data.model

/**
 * Simplified album model for WearOS.
 */
data class WearAlbum(
    val id: String,
    val title: String,
    val artist: String?,
    val year: Int?,
    val thumbnailUrl: String?
)
