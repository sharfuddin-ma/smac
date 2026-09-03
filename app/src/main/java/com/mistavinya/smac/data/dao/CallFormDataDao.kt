package com.mistavinya.smac.data.dao

import androidx.room.*
import com.mistavinya.smac.data.entity.CallFormDataEntity

@Dao
interface CallFormDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(formData: CallFormDataEntity): Long

    @Update
    suspend fun update(formData: CallFormDataEntity)

    @Query("SELECT * FROM call_form_data WHERE call_log_id = :callLogId")
    suspend fun getByCallLogId(callLogId: String): CallFormDataEntity?

    @Query("SELECT * FROM call_form_data WHERE is_synced = 0")
    suspend fun getUnsynced(): List<CallFormDataEntity>

    @Query("UPDATE call_form_data SET is_synced = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun markSynced(id: String, updatedAt: Long = System.currentTimeMillis())
}
