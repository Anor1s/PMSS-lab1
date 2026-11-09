package com.example.cargotiltmonitor.repository

data class SensorReading(
    val x: Float,
    val y: Float,
    val z: Float,
    val tilt: Float,
    val timestamp: Long = System.currentTimeMillis()
)

fun SensorReading.toPayload() = SensorPayload(
    deviceId = CloudConfig.DEVICE_ID,
    sensorType = CloudConfig.SENSOR_TYPE,
    x = x, y = y, z = z, tilt = tilt,
    timestamp = timestamp
)