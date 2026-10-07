package com.nora.tunnel.data.repository

import com.nora.tunnel.data.database.ConnectionSession
import com.nora.tunnel.data.database.SessionDao
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: SessionDao) {
    fun observe(): Flow<List<ConnectionSession>> = dao.observeAll()
    suspend fun add(session: ConnectionSession) = dao.insert(session)
}
