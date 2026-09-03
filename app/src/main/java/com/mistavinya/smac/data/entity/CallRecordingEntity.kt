package com.mistavinya.smac.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "call_recordings",
    foreignKeys = [ForeignKey(
        entity = CallLogEntity::class,
        parentColumns = ["id"],
        childColumns = ["call_log_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["call_log_id"], unique = true), Index(value = ["upload_status"])]
)
data class CallRecordingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "call_log_id")
    val callLogId: String,

    @ColumnInfo(name = "device_serial")
    val deviceSerial: String,

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String? = null,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long? = null,

    @ColumnInfo(name = "s3_bucket")
    val s3Bucket: String? = null,

    @ColumnInfo(name = "s3_key")
    val s3Key: String? = null,

    @ColumnInfo(name = "upload_status")
    val uploadStatus: String = "PENDING",

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "max_retries")
    val maxRetries: Int = 5,

    @ColumnInfo(name = "next_retry_at")
    val nextRetryAt: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
