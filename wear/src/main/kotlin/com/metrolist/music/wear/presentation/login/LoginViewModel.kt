package com.metrolist.music.wear.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.music.wear.auth.OAuth2Repository
import com.metrolist.music.wear.auth.OAuth2Service
import com.metrolist.music.wear.auth.model.AuthState
import com.metrolist.music.wear.auth.model.TokenPollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val oauthService: OAuth2Service,
    private val oauthRepository: OAuth2Repository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * Start the device code flow.
     */
    fun startLogin() {
        if (_authState.value is AuthState.RequestingCode ||
            _authState.value is AuthState.Polling) {
            return // Already in progress
        }

        viewModelScope.launch {
            _authState.value = AuthState.RequestingCode

            oauthService.requestDeviceCode()
                .onSuccess { response ->
                    Timber.d("Device code received: ${response.userCode}")
                    _authState.value = AuthState.WaitingForAuthorization(
                        userCode = response.userCode,
                        verificationUrl = response.verificationUrl,
                        expiresInSeconds = response.expiresIn
                    )

                    // Start polling for token
                    pollForToken(
                        deviceCode = response.deviceCode,
                        interval = response.interval,
                        expiresIn = response.expiresIn
                    )
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to get device code")
                    _authState.value = AuthState.Error(e.message ?: "Failed to get device code")
                }
        }
    }

    private fun pollForToken(deviceCode: String, interval: Int, expiresIn: Int) {
        viewModelScope.launch {
            _authState.value = when (val current = _authState.value) {
                is AuthState.WaitingForAuthorization -> current // Keep showing code while polling
                else -> AuthState.Polling
            }

            oauthService.pollForToken(deviceCode, interval, expiresIn).collect { result ->
                when (result) {
                    is TokenPollResult.Success -> {
                        Timber.d("Token received, saving...")
                        oauthRepository.saveTokens(result.token)
                        YouTube.bearerToken = result.token.accessToken
                        _authState.value = AuthState.Authenticated
                    }
                    is TokenPollResult.Pending -> {
                        // Keep showing the code, user hasn't authorized yet
                        Timber.d("Still waiting for user authorization...")
                    }
                    is TokenPollResult.Expired -> {
                        Timber.w("Device code expired")
                        _authState.value = AuthState.Expired
                    }
                    is TokenPollResult.Error -> {
                        Timber.e("Token poll error: ${result.message}")
                        _authState.value = AuthState.Error(result.message)
                    }
                }
            }
        }
    }

    /**
     * Retry login after error or expiry.
     */
    fun retry() {
        _authState.value = AuthState.Idle
        startLogin()
    }

    /**
     * Cancel login and go back.
     */
    fun cancel() {
        _authState.value = AuthState.Idle
    }
}
