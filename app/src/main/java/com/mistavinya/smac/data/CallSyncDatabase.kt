package com.mistavinya.smac.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mistavinya.smac.data.dao.CallFormDataDao
import com.mistavinya.smac.data.dao.CallLogDao
import com.mistavinya.smac.data.dao.CallRecordingDao
import com.mistavinya.smac.data.dao.UploadQueueDao
import com.mistavinya.smac.data.entity.CallFormDataEntity
import com.mistavinya.smac.data.entity.CallLogEntity
import com.mistavinya.smac.data.entity.CallRecordingEntity
import com.mistavinya.smac.data.entity.UploadQueueEntity

@Database(
    entities = [
        CallLogEntity::class,
        CallFormDataEntity::class,
        CallRecordingEntity::class,
        UploadQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CallSyncDatabase : RoomDatabase() {

    abstract fun callLogDao(): CallLogDao
    abstract fun callFormDataDao(): CallFormDataDao
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun uploadQueueDao(): UploadQueueDao

    companion object {
        @Volatile
        private var INSTANCE: CallSyncDatabase? = null

        fun getInstance(context: Context): CallSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallSyncDatabase::class.java,
                    "callsync_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
