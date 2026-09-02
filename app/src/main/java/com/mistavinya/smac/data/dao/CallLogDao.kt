package com.mistavinya.smac.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mistavinya.smac.data.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: CallLogEntity): Long

    @Update
    suspend fun update(callLog: CallLogEntity)

    @Delete
    suspend fun delete(callLog: CallLogEntity)

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getById(id: Long): CallLogEntity?

    @Query("SELECT * FROM call_logs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE category = :category ORDER BY createdAt DESC")
    fun getByCategory(category: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE contactName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' OR companyName LIKE '%' || :query || '%'")
    fun searchByNameOrNumber(query: String): Flow<List<CallLogEntity>>

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM call_logs WHERE date = :date ORDER BY time DESC")
    fun getCallsByDate(date: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE phoneNumber = :phoneNumber AND category = 'client' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastClientCallByNumber(phoneNumber: String): CallLogEntity?

    @Query("SELECT * FROM call_logs WHERE storageStatus = 'saved' ORDER BY createdAt DESC")
    fun getAllSavedRecordings(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE date = :todayDate")
    fun getTodayCallCount(todayDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_logs WHERE recordingFilePath != ''")
    fun getTotalRecordingsCount(): Flow<Int>

    @Query("UPDATE call_logs SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("SELECT * FROM call_logs WHERE callType = :type ORDER BY createdAt DESC")
    fun getByCallType(type: String): Flow<List<CallLogEntity>>

    @Query("UPDATE call_logs SET recordingFilePath = :path WHERE id = :id")
    suspend fun updateFilePath(id: Long, path: String)

    @Query("UPDATE call_logs SET companyName = :companyName, contactPersonName = :contactPersonName, callPurpose = :callPurpose, notes = :notes WHERE id = :id")
    suspend fun updateClientDetails(id: Long, companyName: String, contactPersonName: String, callPurpose: String, notes: String)

    @Query("UPDATE call_logs SET companyName = :company, contactPersonName = :name, contactDesignation = :designation WHERE id = :id")
    suspend fun updateClientInfo(id: Long, company: String?, name: String?, designation: String?)
}
