package com.mistavinya.smac.data.dao

import androidx.room.*
import com.mistavinya.smac.data.entity.CallRecordingEntity

@Dao
interface CallRecordingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Update
    suspend fun update(recording: CallRecordingEntity)

    @Query("SELECT * FROM call_recordings WHERE call_log_id = :callLogId")
    suspend fun getByCallLogId(callLogId: String): CallRecordingEntity?

    @Query("SELECT * FROM call_recordings WHERE upload_status = :status")
    suspend fun getByUploadStatus(status: String): List<CallRecordingEntity>

    @Query("SELECT * FROM call_recordings WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CallRecordingEntity>

    @Query("UPDATE call_recordings SET upload_status = :status, error_message = :error, retry_count = retry_count + 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateUploadStatus(id: String, status: String, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE call_recordings SET is_synced = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long = System.currentTimeMillis())
}
