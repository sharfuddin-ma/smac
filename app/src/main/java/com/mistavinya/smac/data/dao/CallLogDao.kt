package com.mistavinya.smac.data.dao

import androidx.room.*
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

    @Query("SELECT * FROM call_logs ORDER BY created_at DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getById(id: String): CallLogEntity?

    @Query("SELECT * FROM call_logs WHERE call_direction = :direction ORDER BY created_at DESC")
    fun getByDirection(direction: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE call_category = :category ORDER BY created_at DESC")
    fun getByCategory(category: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE is_synced = 0 ORDER BY created_at ASC")
    suspend fun getUnsynced(): List<CallLogEntity>

    @Query("UPDATE call_logs SET call_category = :category, is_form_required = :formRequired, has_recording = :hasRecording, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCategory(id: String, category: String, formRequired: Boolean, hasRecording: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE call_logs SET is_form_submitted = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markFormSubmitted(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE call_logs SET is_synced = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM call_logs WHERE contact_name LIKE '%' || :query || '%' OR caller_number LIKE '%' || :query || '%' OR callee_number LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<CallLogEntity>>

    // Convenience for stats
    @Query("SELECT COUNT(*) FROM call_logs WHERE DATE(created_at/1000, 'unixepoch', 'localtime') = :date")
    fun getTodayCallCount(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_logs WHERE has_recording = 1")
    fun getTotalRecordingsCount(): Flow<Int>

    @Query("SELECT * FROM call_logs WHERE has_recording = 1 ORDER BY created_at DESC")
    fun getAllSavedRecordings(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY created_at DESC LIMIT :limit")
    fun getRecentCalls(limit: Int): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE created_at >= :since")
    fun getCallCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM call_logs")
    fun getTotalCallCount(): Flow<Int>

    @Query("""
        UPDATE call_logs SET 
            has_recording = :hasRecording, 
            local_recording_path = :localRecordingPath,
            recording_file_name = :recordingFileName,
            recording_file_size_bytes = :recordingFileSizeBytes 
        WHERE id = :callId
    """)
    suspend fun updateRecording(
        callId: String,
        hasRecording: Boolean,
        localRecordingPath: String?,
        recordingFileName: String?,
        recordingFileSizeBytes: Long
    )
}
