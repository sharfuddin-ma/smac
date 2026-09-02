package com.mistavinya.smac.data.repository

import com.mistavinya.smac.data.dao.CallLogDao
import com.mistavinya.smac.data.entity.CallLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallLogRepository(private val callLogDao: CallLogDao) {

    suspend fun insert(callLog: CallLogEntity): Long = withContext(Dispatchers.IO) {
        callLogDao.insert(callLog)
    }

    suspend fun update(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.update(callLog)
    }

    suspend fun delete(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        callLogDao.delete(callLog)
    }

    suspend fun getById(id: Long): CallLogEntity? = withContext(Dispatchers.IO) {
        callLogDao.getById(id)
    }

    fun getAll(): Flow<List<CallLogEntity>> = callLogDao.getAll()

    fun getByCategory(category: String): Flow<List<CallLogEntity>> = callLogDao.getByCategory(category)

    fun searchByNameOrNumber(query: String): Flow<List<CallLogEntity>> = callLogDao.searchByNameOrNumber(query)

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        callLogDao.deleteById(id)
    }

    fun getCallsByDate(date: String): Flow<List<CallLogEntity>> = callLogDao.getCallsByDate(date)

    fun getRecentCalls(limit: Int): Flow<List<CallLogEntity>> = callLogDao.getRecentCalls(limit)

    suspend fun getLastClientCallByNumber(phoneNumber: String): CallLogEntity? = withContext(Dispatchers.IO) {
        callLogDao.getLastClientCallByNumber(phoneNumber)
    }

    fun getAllSavedRecordings(): Flow<List<CallLogEntity>> = callLogDao.getAllSavedRecordings()

    fun getTodayCallCount(todayDate: String): Flow<Int> = callLogDao.getTodayCallCount(todayDate)

    fun getTotalRecordingsCount(): Flow<Int> = callLogDao.getTotalRecordingsCount()

    fun getByCallType(type: String): Flow<List<CallLogEntity>> = callLogDao.getByCallType(type)

    suspend fun updateCategory(id: Long, category: String) = withContext(Dispatchers.IO) {
        callLogDao.updateCategory(id, category)
    }

    suspend fun updateFilePath(id: Long, path: String) = withContext(Dispatchers.IO) {
        callLogDao.updateFilePath(id, path)
    }
}
