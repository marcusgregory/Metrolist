package com.metrolist.music.wear.presentation.volume

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.AudioOutputRepository
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.audio.VolumeState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class VolumeViewModel @Inject constructor(
    private val audioOutputRepository: AudioOutputRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val systemRepo = audioOutputRepository as? SystemAudioRepository

    val audioOutput: StateFlow<AudioOutput> = audioOutputRepository.audioOutput
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AudioOutput.None
        )

    val availableOutputs: StateFlow<List<AudioOutput>> = audioOutputRepository.available
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val volumeState: StateFlow<VolumeState> = systemRepo
        ?.volumeState
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VolumeState(0, 15)
        ) ?: kotlinx.coroutines.flow.MutableStateFlow(VolumeState(0, 15))

    fun increaseVolume() {
        systemRepo?.increaseVolume()
    }

    fun decreaseVolume() {
        systemRepo?.decreaseVolume()
    }

    fun launchOutputSelection() {
        // Open Bluetooth settings on WearOS
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
