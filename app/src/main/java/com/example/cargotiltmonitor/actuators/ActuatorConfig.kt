package com.example.cargotiltmonitor.actuators

object ActuatorConfig {
    const val TILT_THRESHOLD_DEG = 30f
    const val HYSTERESIS_DEG = 3f
    const val CMD_ON = "on"
    val ALARM_PATTERN = longArrayOf(0, 400, 300) // пауза, вібро, пауза; повторюється
}