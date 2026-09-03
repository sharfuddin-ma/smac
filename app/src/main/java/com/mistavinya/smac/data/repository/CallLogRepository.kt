package com.mistavinya.smac.data.repository

import com.mistavinya.smac.data.dao.CallLogDao
import com.mistavinya.smac.data.entity.CallLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(private val callLogDao: CallLogDao) {

    suspend fun insert(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.insert(callLog)
    }

    suspend fun update(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.update(callLog)
    }

    suspend fun delete(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.delete(callLog)
    }

    suspend fun getById(id: String): CallLogEntity? = withContext(Dispatchers.IO) {
        callLogDao.getById(id)
    }

    fun getAll(): Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()

    fun getByCategory(category: String): Flow<List<CallLogEntity>> = callLogDao.getByCategory(category)

    fun search(query: String): Flow<List<CallLogEntity>> = callLogDao.search(query)

    fun getByDirection(direction: String): Flow<List<CallLogEntity>> = callLogDao.getByDirection(direction)

    suspend fun getUnsynced(): List<CallLogEntity> = withContext(Dispatchers.IO) {
        callLogDao.getUnsynced()
    }

    suspend fun updateCategory(id: String, category: String, formRequired: Boolean, hasRecording: Boolean) = withContext(Dispatchers.IO) {
        callLogDao.updateCategory(id, category, formRequired, hasRecording)
    }

    suspend fun markFormSubmitted(id: String) = withContext(Dispatchers.IO) {
        callLogDao.markFormSubmitted(id)
    }

    suspend fun markSynced(id: String) = withContext(Dispatchers.IO) {
        callLogDao.markSynced(id)
    }

    // New convenience methods for UI stats
    fun getTodayCallCount(date: String) = callLogDao.getTodayCallCount(date)
    fun getCallCountSince(since: Long) = callLogDao.getCallCountSince(since)
    fun getTotalCallCount() = callLogDao.getTotalCallCount()
    fun getTotalRecordingsCount() = callLogDao.getTotalRecordingsCount()
    fun getAllSavedRecordings() = callLogDao.getAllSavedRecordings()
    fun getRecentCalls(limit: Int) = callLogDao.getRecentCalls(limit)
}
