package com.mistavinya.smac.storage

import android.content.Context
import com.google.gson.Gson
import com.mistavinya.smac.data.entity.CallLogEntity
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

class LocalStorageManager(private val context: Context) {
    private val gson = Gson()
    private val baseDir = File(context.filesDir, "CallSync")
    private val recordingsDir = File(baseDir, "recordings")
    private val exportsDir = File(baseDir, "exports")
    private val tempDir = File(baseDir, "temp")

    private val categoryDirs = mapOf(
        "client" to File(recordingsDir, "client"),
        "team_member" to File(recordingsDir, "team_member"),
        "unclassified" to File(recordingsDir, "unclassified")
    )

    init {
        if (!baseDir.exists()) baseDir.mkdir()
        if (!recordingsDir.exists()) recordingsDir.mkdir()
        if (!exportsDir.exists()) exportsDir.mkdir()
        if (!tempDir.exists()) tempDir.mkdir()
        categoryDirs.values.forEach { if (!it.exists()) it.mkdir() }
    }

    fun createRecordingFile(phoneNumber: String): File {
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())
        val sanitizedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        val fileName = "${timestamp}_${sanitizedNumber}.mp4"
        val targetTempDir = File(context.filesDir, "CallSync/temp")
        if (!targetTempDir.exists()) targetTempDir.mkdirs()
        return File(targetTempDir, fileName)
    }

    fun moveRecordingToCategory(tempFile: File, category: String, phoneNumber: String): File {
        val targetDir = categoryDirs[category] ?: categoryDirs["unclassified"]!!
        val newFile = File(targetDir, tempFile.name)
        if (tempFile.exists()) {
            tempFile.renameTo(newFile)
        }
        return newFile
    }

    fun saveMetadataJson(callLogEntity: CallLogEntity, category: String): File {
        val targetDir = categoryDirs[category] ?: categoryDirs["unclassified"]!!
        val jsonFile = File(targetDir, "METADATA_${callLogEntity.phoneNumber}_${callLogEntity.id}.json")
        val jsonString = gson.toJson(callLogEntity)
        
        FileOutputStream(jsonFile).use { fos ->
            fos.write(jsonString.toByteArray(StandardCharsets.UTF_8))
        }
        return jsonFile
    }

    fun deleteRecording(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    fun getStorageUsed(): Long {
        return calculateDirSize(baseDir)
    }

    fun getStorageUsedFormatted(): String {
        val bytes = getStorageUsed()
        val mb = bytes / (1024 * 1024)
        return "$mb MB"
    }

    fun getAllRecordingFiles(): List<File> {
        val allFiles = mutableListOf<File>()
        categoryDirs.values.forEach { dir ->
            dir.listFiles()?.filter { it.extension == "mp4" }?.let { allFiles.addAll(it) }
        }
        return allFiles
    }

    fun clearOldRecordings(daysOld: Int): Int {
        var deletedCount = 0
        val cutoff = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
        getAllRecordingFiles().forEach { file ->
            if (file.lastModified() < cutoff) {
                if (file.delete()) deletedCount++
            }
        }
        return deletedCount
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }
}
