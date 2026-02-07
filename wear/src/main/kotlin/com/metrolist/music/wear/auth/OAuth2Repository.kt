package com.metrolist.music.wear.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.wear.auth.model.TokenResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for storing and retrieving OAuth tokens.
 */
@Singleton
class OAuth2Repository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val TOKEN_EXPIRY_KEY = longPreferencesKey("token_expiry")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")

        // Buffer time before token expiry (5 minutes)
        private const val EXPIRY_BUFFER_MS = 5 * 60 * 1000L
    }

    /**
     * Save tokens from OAuth response.
     */
    suspend fun saveTokens(response: TokenResponse) {
        val expiryTime = System.currentTimeMillis() + (response.expiresIn * 1000L)

        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = response.accessToken
            response.refreshToken?.let { prefs[REFRESH_TOKEN_KEY] = it }
            prefs[TOKEN_EXPIRY_KEY] = expiryTime
            prefs[TOKEN_TYPE_KEY] = response.tokenType
        }

        Timber.d("Tokens saved, expires at: $expiryTime")
    }

    /**
     * Get the current access token if available.
     */
    suspend fun getAccessToken(): String? {
        return dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    /**
     * Get the refresh token if available.
     */
    suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    /**
     * Check if the current token is expired or about to expire.
     */
    suspend fun isTokenExpired(): Boolean {
        val expiry = dataStore.data.first()[TOKEN_EXPIRY_KEY] ?: return true
        val now = System.currentTimeMillis()
        return now >= (expiry - EXPIRY_BUFFER_MS)
    }

    /**
     * Get token expiry time.
     */
    suspend fun getTokenExpiry(): Long? {
        return dataStore.data.first()[TOKEN_EXPIRY_KEY]
    }

    /**
     * Check if user is logged in (has tokens).
     */
    suspend fun isLoggedIn(): Boolean {
        val accessToken = getAccessToken()
        val refreshToken = getRefreshToken()
        return !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
    }

    /**
     * Flow to observe login state changes.
     */
    fun observeLoginState(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            val accessToken = prefs[ACCESS_TOKEN_KEY]
            val refreshToken = prefs[REFRESH_TOKEN_KEY]
            !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
        }
    }

    /**
     * Clear all tokens (logout).
     */
    suspend fun clearTokens() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(REFRESH_TOKEN_KEY)
            prefs.remove(TOKEN_EXPIRY_KEY)
            prefs.remove(TOKEN_TYPE_KEY)
        }
        Timber.d("Tokens cleared")
    }
}
