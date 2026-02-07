package com.metrolist.music.wear.presentation.search

/**
 * Simplified song model for WearOS.
 */
data class WearSong(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Int?
)
