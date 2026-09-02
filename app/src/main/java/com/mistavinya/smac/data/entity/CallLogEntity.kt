package com.mistavinya.smac.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: String, // "incoming" or "outgoing"
    val date: String, // "YYYY-MM-DD"
    val time: String, // "HH:mm:ss"
    val durationSeconds: Long,
    val recordingFilePath: String,
    val category: String? = null, // "personal", "team_member", "client"
    val storageStatus: String = "saved", // "saved", "deleted"
    val companyName: String? = null,
    val contactPersonName: String? = null,
    val contactDesignation: String? = null,
    val callPurpose: String? = null,
    val callOutcome: String? = null,
    val assignedTo: String? = null,
    val followUpRequired: Boolean = false,
    val followUpDate: String? = null,
    val notes: String? = null,
    val dealName: String? = null,
    val estimatedValue: Double? = null,
    val priority: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
