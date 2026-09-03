package com.mistavinya.smac.data.dao

import androidx.room.*
import com.mistavinya.smac.data.entity.UploadQueueEntity

@Dao
interface UploadQueueDao {
    @Insert
    suspend fun insert(item: UploadQueueEntity): Long

    @Update
    suspend fun update(item: UploadQueueEntity)

    @Delete
    suspend fun delete(item: UploadQueueEntity)

    @Query("SELECT * FROM upload_queue WHERE status = 'QUEUED' ORDER BY created_at ASC")
    suspend fun getQueued(): List<UploadQueueEntity>

    @Query("SELECT * FROM upload_queue WHERE status = 'FAILED' AND retry_count < max_retries ORDER BY created_at ASC")
    suspend fun getRetryable(): List<UploadQueueEntity>

    @Query("SELECT * FROM upload_queue WHERE call_log_local_id = :callLogId ORDER BY created_at ASC")
    suspend fun getByCallLogId(callLogId: String): List<UploadQueueEntity>

    @Query("UPDATE upload_queue SET status = :status, error_message = :error, retry_count = retry_count + 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM upload_queue WHERE status = 'COMPLETED'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM upload_queue WHERE call_log_local_id = :callLogId AND upload_type = :type")
    suspend fun deleteByCallLogIdAndType(callLogId: String, type: String)
}
