package com.metrolist.music.wear.data.model

/**
 * Simplified artist model for WearOS.
 */
data class WearArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscriberCount: String?
)
