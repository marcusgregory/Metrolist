package com.metrolist.music.wear.data.repository

import com.metrolist.music.wear.data.db.HistoryDao
import com.metrolist.music.wear.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getRecentHistory(): Flow<List<HistoryEntity>> = historyDao.getRecent()

    suspend fun addToHistory(
        id: String,
        title: String,
        artist: String,
        thumbnailUrl: String?
    ) {
        historyDao.upsert(
            HistoryEntity(
                id = id,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                playedAt = System.currentTimeMillis()
            )
        )
        historyDao.pruneOld()
    }
}
