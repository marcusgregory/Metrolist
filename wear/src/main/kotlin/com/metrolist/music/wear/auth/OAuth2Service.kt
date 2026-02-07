package com.metrolist.music.wear.auth

import com.metrolist.music.wear.auth.model.DeviceCodeResponse
import com.metrolist.music.wear.auth.model.TokenErrorResponse
import com.metrolist.music.wear.auth.model.TokenPollResult
import com.metrolist.music.wear.auth.model.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for OAuth2 Device Code Flow operations.
 * Uses dynamic credentials extracted from YouTube's base.js file.
 */
@Singleton
class OAuth2Service @Inject constructor(
    private val httpClient: HttpClient,
    private val dynamicOAuthService: DynamicOAuthService
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Cached credentials
    private var cachedCredentials: DynamicOAuthService.OAuthCredentials? = null

    /**
     * Get OAuth credentials, fetching dynamically if not cached.
     */
    private suspend fun getCredentials(): DynamicOAuthService.OAuthCredentials {
        cachedCredentials?.let { return it }

        val credentials = dynamicOAuthService.fetchCredentials().getOrThrow()
        cachedCredentials = credentials
        return credentials
    }

    /**
     * Request a device code for user authorization.
     */
    suspend fun requestDeviceCode(): Result<DeviceCodeResponse> = runCatching {
        Timber.d("Requesting device code...")

        // Get dynamic credentials from YouTube
        val credentials = getCredentials()
        Timber.d("Using dynamic client_id: ${credentials.clientId.take(20)}...")

        val response = httpClient.submitForm(
            url = OAuth2Config.DEVICE_CODE_URL,
            formParameters = Parameters.build {
                append("client_id", credentials.clientId)
                append("scope", OAuth2Config.SCOPE)
            }
        )

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Device code response: $body")
            json.decodeFromString<DeviceCodeResponse>(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Device code request failed: ${response.status} - $errorBody")
            throw Exception("Failed to get device code: ${response.status}")
        }
    }

    /**
     * Poll for token after user authorization.
     * Emits TokenPollResult.Pending while waiting, Success when authorized, or Error/Expired on failure.
     */
    fun pollForToken(
        deviceCode: String,
        interval: Int,
        expiresIn: Int
    ): Flow<TokenPollResult> = flow {
        val intervalMs = (interval * 1000).toLong()
        val startTime = System.currentTimeMillis()
        val expiryTime = startTime + (expiresIn * 1000)

        // Get credentials (should already be cached from requestDeviceCode)
        val credentials = try {
            getCredentials()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get credentials for polling")
            emit(TokenPollResult.Error("Failed to get credentials: ${e.message}"))
            return@flow
        }

        while (System.currentTimeMillis() < expiryTime) {
            delay(intervalMs)

            try {
                val response = httpClient.submitForm(
                    url = OAuth2Config.TOKEN_URL,
                    formParameters = Parameters.build {
                        append("client_id", credentials.clientId)
                        append("client_secret", credentials.clientSecret)
                        append("code", deviceCode)
                        append("grant_type", OAuth2Config.GRANT_TYPE_DEVICE_CODE)
                    }
                )

                val body = response.bodyAsText()
                Timber.d("Poll response: $body")

                // Try to parse as error first (YouTube returns 200 OK even for errors)
                val error = runCatching {
                    json.decodeFromString<TokenErrorResponse>(body)
                }.getOrNull()

                if (error?.error != null) {
                    // It's an error response
                    when (error.error) {
                        "authorization_pending" -> {
                            Timber.d("Authorization pending, continuing to poll...")
                            emit(TokenPollResult.Pending)
                        }
                        "slow_down" -> {
                            Timber.d("Slow down requested, adding delay...")
                            delay(5000) // Add 5 seconds extra
                            emit(TokenPollResult.Pending)
                        }
                        "expired_token" -> {
                            Timber.w("Device code expired")
                            emit(TokenPollResult.Expired)
                            return@flow
                        }
                        "access_denied" -> {
                            Timber.w("Access denied by user")
                            emit(TokenPollResult.Error("Access denied by user"))
                            return@flow
                        }
                        else -> {
                            Timber.e("Token poll error: ${error.error} - ${error.errorDescription}")
                            emit(TokenPollResult.Error(error.errorDescription ?: "Unknown error"))
                            return@flow
                        }
                    }
                } else {
                    // Not an error, try to parse as token
                    val token = runCatching {
                        json.decodeFromString<TokenResponse>(body)
                    }.getOrNull()

                    if (token != null) {
                        Timber.d("Token received successfully")
                        emit(TokenPollResult.Success(token))
                        return@flow
                    } else {
                        Timber.e("Failed to parse response: $body")
                        emit(TokenPollResult.Error("Failed to parse server response"))
                        return@flow
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error polling for token")
                emit(TokenPollResult.Error(e.message ?: "Network error"))
                return@flow
            }
        }

        // Expired
        emit(TokenPollResult.Expired)
    }

    /**
     * Refresh an expired access token.
     */
    suspend fun refreshToken(refreshToken: String): Result<TokenResponse> = runCatching {
        Timber.d("Refreshing token...")

        val credentials = getCredentials()

        val response = httpClient.submitForm(
            url = OAuth2Config.TOKEN_URL,
            formParameters = Parameters.build {
                append("client_id", credentials.clientId)
                append("client_secret", credentials.clientSecret)
                append("refresh_token", refreshToken)
                append("grant_type", OAuth2Config.GRANT_TYPE_REFRESH)
            }
        )

        if (response.status.isSuccess()) {
            val body = response.bodyAsText()
            Timber.d("Token refreshed successfully")
            json.decodeFromString<TokenResponse>(body)
        } else {
            val errorBody = response.bodyAsText()
            Timber.e("Token refresh failed: ${response.status} - $errorBody")
            throw Exception("Failed to refresh token: ${response.status}")
        }
    }

    /**
     * Clear cached credentials (useful for debugging or forcing refresh).
     */
    fun clearCredentialsCache() {
        cachedCredentials = null
    }
}
