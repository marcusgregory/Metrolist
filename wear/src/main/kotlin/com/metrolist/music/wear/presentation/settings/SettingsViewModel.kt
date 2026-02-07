package com.metrolist.music.wear.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.wear.auth.AccountInfo
import com.metrolist.music.wear.auth.AccountService
import com.metrolist.music.wear.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val accountService: AccountService
) : ViewModel() {

    private val _accountInfo = MutableStateFlow<SettingsAccountInfo?>(null)
    val accountInfo: StateFlow<SettingsAccountInfo?> = _accountInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _logoutComplete = MutableStateFlow(false)
    val logoutComplete: StateFlow<Boolean> = _logoutComplete.asStateFlow()

    init {
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = accountService.getAccountInfo()
                result.onSuccess { info ->
                    Timber.d("Account info loaded: ${info.name}")
                    _accountInfo.value = SettingsAccountInfo(
                        name = info.name,
                        email = info.email,
                        channelHandle = null,
                        thumbnailUrl = info.thumbnailUrl
                    )
                }.onFailure { error ->
                    Timber.e(error, "Failed to load account info")
                    _accountInfo.value = null
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception loading account info")
                _accountInfo.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Timber.d("Logging out...")
            tokenManager.logout()
            _logoutComplete.value = true
        }
    }
}

data class SettingsAccountInfo(
    val name: String,
    val email: String?,
    val channelHandle: String?,
    val thumbnailUrl: String?
)
