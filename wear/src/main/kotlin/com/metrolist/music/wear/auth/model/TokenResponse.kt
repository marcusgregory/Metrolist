package com.metrolist.music.wear.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from token request (both initial and refresh).
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("expires_in")
    val expiresIn: Int,

    @SerialName("token_type")
    val tokenType: String = "Bearer",

    @SerialName("scope")
    val scope: String? = null
)

/**
 * Error response from OAuth endpoints.
 */
@Serializable
data class TokenErrorResponse(
    @SerialName("error")
    val error: String,

    @SerialName("error_description")
    val errorDescription: String? = null
)
