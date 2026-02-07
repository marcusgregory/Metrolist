package com.metrolist.music.wear.auth

import com.metrolist.innertube.YouTube
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages OAuth tokens with automatic refresh.
 */
@Singleton
class TokenManager @Inject constructor(
    private val repository: OAuth2Repository,
    private val service: OAuth2Service
) {
    private val refreshMutex = Mutex()

    /**
     * Get a valid access token, refreshing if necessary.
     * Returns null if not logged in or refresh fails.
     */
    suspend fun getValidToken(): String? {
        if (!repository.isLoggedIn()) {
            Timber.d("Not logged in, no token available")
            return null
        }

        // Check if token needs refresh
        if (repository.isTokenExpired()) {
            Timber.d("Token expired, attempting refresh...")
            val refreshed = refreshToken()
            if (!refreshed) {
                Timber.w("Token refresh failed")
                return null
            }
        }

        return repository.getAccessToken()
    }

    /**
     * Refresh the access token using the refresh token.
     * Thread-safe to prevent multiple concurrent refresh attempts.
     */
    suspend fun refreshToken(): Boolean = refreshMutex.withLock {
        // Double-check after acquiring lock
        if (!repository.isTokenExpired()) {
            Timber.d("Token was refreshed by another call")
            return true
        }

        val refreshToken = repository.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Timber.w("No refresh token available")
            return false
        }

        return service.refreshToken(refreshToken)
            .onSuccess { response ->
                repository.saveTokens(response)
                // Update YouTube singleton with new token
                YouTube.bearerToken = response.accessToken
                Timber.d("Token refreshed successfully")
            }
            .onFailure { e ->
                Timber.e(e, "Failed to refresh token")
                // If refresh fails, clear tokens to force re-login
                repository.clearTokens()
                YouTube.bearerToken = null
            }
            .isSuccess
    }

    /**
     * Initialize the token manager on app start.
     * Sets the bearer token on the YouTube singleton if logged in.
     */
    suspend fun initialize() {
        val token = getValidToken()
        if (token != null) {
            YouTube.bearerToken = token
            Timber.d("TokenManager initialized with valid token")
        } else {
            Timber.d("TokenManager initialized without token")
        }
    }

    /**
     * Handle 401 Unauthorized responses by attempting token refresh.
     */
    suspend fun handleUnauthorized(): Boolean {
        Timber.d("Handling 401 Unauthorized, forcing token refresh...")
        return refreshToken()
    }

    /**
     * Logout and clear all tokens.
     */
    suspend fun logout() {
        repository.clearTokens()
        YouTube.bearerToken = null
        Timber.d("Logged out, tokens cleared")
    }

    /**
     * Check if user is logged in.
     */
    suspend fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
