package com.mistavinya.smac.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "call_form_data",
    foreignKeys = [ForeignKey(
        entity = CallLogEntity::class,
        parentColumns = ["id"],
        childColumns = ["call_log_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["call_log_id"], unique = true)]
)
data class CallFormDataEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "call_log_id")
    val callLogId: String,

    @ColumnInfo(name = "company_name")
    val companyName: String,

    @ColumnInfo(name = "customer_name")
    val customerName: String,

    @ColumnInfo(name = "reason_for_call")
    val reasonForCall: String? = null,

    val notes: String? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
