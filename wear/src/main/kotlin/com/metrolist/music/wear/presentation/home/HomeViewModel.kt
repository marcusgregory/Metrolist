package com.metrolist.music.wear.presentation.home

import androidx.lifecycle.ViewModel
import com.metrolist.music.wear.data.db.HistoryEntity
import com.metrolist.music.wear.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val recentHistory: Flow<List<HistoryEntity>> = historyRepository.getRecentHistory()
}
