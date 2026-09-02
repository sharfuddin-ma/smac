package com.mistavinya.smac.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mistavinya.smac.data.dao.CallLogDao
import com.mistavinya.smac.data.entity.CallLogEntity

@Database(entities = [CallLogEntity::class], version = 2, exportSchema = false)
abstract class CallSyncDatabase : RoomDatabase() {

    abstract fun callLogDao(): CallLogDao

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
