package com.mistavinya.smac.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "device_serial")
    val deviceSerial: String,

    @ColumnInfo(name = "call_direction")
    val callDirection: String,           // INCOMING | OUTGOING | MISSED

    @ColumnInfo(name = "caller_number")
    val callerNumber: String,

    @ColumnInfo(name = "callee_number")
    val calleeNumber: String,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0,

    @ColumnInfo(name = "call_category")
    val callCategory: String = "PENDING", // CLIENT | TEAM_MEMBER | PERSONAL | MISSED | PENDING

    @ColumnInfo(name = "is_form_required")
    val isFormRequired: Boolean = false,

    @ColumnInfo(name = "is_form_submitted")
    val isFormSubmitted: Boolean = false,

    @ColumnInfo(name = "has_recording")
    var hasRecording: Boolean = false,

    @ColumnInfo(name = "local_recording_path")
    var localRecordingPath: String? = null,

    @ColumnInfo(name = "recording_file_name")
    var recordingFileName: String? = null,

    @ColumnInfo(name = "recording_file_size_bytes")
    var recordingFileSizeBytes: Long = 0,

    @ColumnInfo(name = "contact_name")
    val contactName: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
