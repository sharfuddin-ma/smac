package com.mistavinya.smac.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

/**
 * Scans the user-mapped Samsung recording folder and matches recordings to calls
 * by phone number in filename + timestamp proximity.
 */
object RecordingMatcher {

    private const val TAG = "RecordingMatcher"

    data class RecordingMatch(
        val uri: Uri,
        val fileName: String,
        val fileSizeBytes: Long,
        val lastModified: Long
    )

    /**
     * Find a recording that matches the given phone number and call time.
     */
    fun findRecording(
        context: Context,
        folderUriString: String,
        phoneNumber: String,
        callEndTimeMillis: Long,
        toleranceMs: Long = 120_000
    ): RecordingMatch? {
        if (folderUriString.isBlank()) {
            Log.w(TAG, "No recording folder mapped")
            return null
        }

        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val last7Digits = cleanNumber.takeLast(7)

        if (last7Digits.length < 4) {
            Log.w(TAG, "Phone number too short: $phoneNumber")
            return null
        }

        try {
            val folderUri = Uri.parse(folderUriString)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getTreeDocumentId(folderUri)
            )

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            val cursor = context.contentResolver.query(
                childrenUri, projection, null, null, null
            ) ?: return null

            val candidates = mutableListOf<RecordingMatch>()

            cursor.use {
                val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val mimeIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (it.moveToNext()) {
                    val mimeType = if (mimeIndex >= 0) it.getString(mimeIndex) ?: "" else ""
                    if (!mimeType.startsWith("audio/")) continue

                    val docId = if (idIndex >= 0) it.getString(idIndex) else continue
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                    val modified = if (modifiedIndex >= 0) it.getLong(modifiedIndex) else 0L

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
                    candidates.add(RecordingMatch(fileUri, name, size, modified))
                }
            }

            if (candidates.isEmpty()) return null

            // Match by phone number in filename + time proximity
            val match = candidates.filter { match ->
                val nameDigits = match.fileName.replace(Regex("[^0-9]"), "")
                val numberMatch = nameDigits.contains(last7Digits)
                val timeMatch = Math.abs(match.lastModified - callEndTimeMillis) < toleranceMs
                numberMatch && timeMatch
            }.sortedByDescending { it.lastModified }.firstOrNull()

            if (match != null) {
                Log.i(TAG, "✅ Found match: ${match.fileName}")
            }
            return match

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder: ${e.message}")
            return null
        }
    }
}
