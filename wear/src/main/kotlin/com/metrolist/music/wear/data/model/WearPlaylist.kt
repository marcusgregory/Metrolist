package com.metrolist.music.wear.data.model

/**
 * Simplified playlist model for WearOS.
 */
data class WearPlaylist(
    val id: String,
    val title: String,
    val author: String?,
    val songCountText: String?,
    val thumbnailUrl: String?
)
