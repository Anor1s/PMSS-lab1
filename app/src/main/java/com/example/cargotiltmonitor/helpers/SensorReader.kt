package com.example.cargotiltmonitor.helpers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// Помічник: перетворює Android API сенсорів на Flow
class SensorReader(context: Context) {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun readings(): Flow<FloatArray> = callbackFlow {
        val listener = object : SensorEventListener {
            // Нові дані з сенсора → у Flow
            override fun onSensorChanged(e: SensorEvent) {
                trySend(e.values.clone())
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        // Реєстрація слухача
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        // Викликається при cancel() → зупинка слухача (енергозбереження)
        awaitClose { manager.unregisterListener(listener) }
    }
}