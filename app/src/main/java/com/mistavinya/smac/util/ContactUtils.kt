package com.mistavinya.smac.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactUtils {
    fun getContactName(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isBlank() || phoneNumber == "Unknown") return null
        
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: SecurityException) {
            android.util.Log.e("ContactUtils", "SecurityException looking up contact: ${e.message}")
            null
        } catch (e: Exception) { 
            android.util.Log.e("ContactUtils", "Error looking up contact: ${e.message}")
            null 
        }
    }
}
