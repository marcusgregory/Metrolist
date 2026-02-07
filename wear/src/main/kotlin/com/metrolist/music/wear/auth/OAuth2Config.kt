package com.metrolist.music.wear.auth

/**
 * OAuth2 configuration for YouTube Device Code Flow.
 * Note: Client ID and Secret are now fetched dynamically from YouTube's base.js
 * using DynamicOAuthService (same approach as SmartTube).
 */
object OAuth2Config {
    // OAuth scope for YouTube access
    const val SCOPE = "https://www.googleapis.com/auth/youtube"

    // OAuth endpoints
    const val DEVICE_CODE_URL = "https://www.youtube.com/o/oauth2/device/code"
    const val TOKEN_URL = "https://www.youtube.com/o/oauth2/token"

    // Grant types
    const val GRANT_TYPE_DEVICE_CODE = "http://oauth.net/grant_type/device/1.0"
    const val GRANT_TYPE_REFRESH = "refresh_token"
}
