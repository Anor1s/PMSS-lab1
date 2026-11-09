package com.example.cargotiltmonitor.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_record")
data class SensorRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,           // мс Unix-епохи
    val sensorValue: Float,        // кут нахилу, градуси
    val actionTriggered: Boolean   // чи були активовані актуатори
)