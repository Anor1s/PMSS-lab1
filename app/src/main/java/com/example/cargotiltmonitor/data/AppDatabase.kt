package com.example.cargotiltmonitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SensorRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sensorDao(): SensorDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // Синглтон: одна база на весь додаток
        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "cargo_tilt.db"
            ).build().also { instance = it }
        }
    }
}