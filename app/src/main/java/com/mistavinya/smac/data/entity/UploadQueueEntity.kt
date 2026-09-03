package com.mistavinya.smac.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "upload_queue",
    indices = [Index("status"), Index("upload_type")]
)
data class UploadQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "call_log_local_id")
    val callLogLocalId: String,

    @ColumnInfo(name = "upload_type")
    val uploadType: String,              // CALL_LOG | FORM_DATA | RECORDING

    val payload: String,                 // JSON string

    @ColumnInfo(name = "file_path")
    val filePath: String = "",

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,

    val status: String = "QUEUED",       // QUEUED | IN_PROGRESS | COMPLETED | FAILED

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 5,

    @ColumnInfo(name = "next_retry_at")
    val nextRetryAt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
