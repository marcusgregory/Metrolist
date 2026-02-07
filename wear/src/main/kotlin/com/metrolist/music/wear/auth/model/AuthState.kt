package com.metrolist.music.wear.auth.model

/**
 * Represents the current authentication state in the login flow.
 */
sealed class AuthState {
    /**
     * Initial state, waiting to start.
     */
    data object Idle : AuthState()

    /**
     * Requesting device code from server.
     */
    data object RequestingCode : AuthState()

    /**
     * Device code received, waiting for user to authorize.
     */
    data class WaitingForAuthorization(
        val userCode: String,
        val verificationUrl: String,
        val expiresInSeconds: Int
    ) : AuthState()

    /**
     * Polling for token after user authorizes.
     */
    data object Polling : AuthState()

    /**
     * Successfully authenticated.
     */
    data object Authenticated : AuthState()

    /**
     * Authentication failed.
     */
    data class Error(val message: String) : AuthState()

    /**
     * Code expired, need to restart.
     */
    data object Expired : AuthState()
}

/**
 * Result of polling for token.
 */
sealed class TokenPollResult {
    data class Success(val token: TokenResponse) : TokenPollResult()
    data object Pending : TokenPollResult()
    data class Error(val message: String) : TokenPollResult()
    data object Expired : TokenPollResult()
}
