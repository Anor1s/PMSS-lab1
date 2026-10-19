package com.example.cargotiltmonitor.helpers

import java.util.UUID

object BleProfile {
    val SERVICE_UUID: UUID = UUID.fromString("0000a100-0000-1000-8000-00805f9b34fb")
    val STATUS_CHAR_UUID: UUID = UUID.fromString("0000a101-0000-1000-8000-00805f9b34fb") // Read + Notify
    val COMMAND_CHAR_UUID: UUID = UUID.fromString("0000a102-0000-1000-8000-00805f9b34fb") // Write

    const val CMD_ALARM_ON = "ALARM_ON"
    const val CMD_ALARM_OFF = "ALARM_OFF"
    const val CMD_PING = "PING"
    val COMMANDS = listOf(CMD_ALARM_ON, CMD_ALARM_OFF, CMD_PING)

    const val SCAN_DELAY_MS = 2000L
    const val SCAN_STEP_MS = 300L
    const val CONNECT_DELAY_MS = 1500L
    const val WRITE_DELAY_MS = 400L
    const val MAX_LOG_LINES = 100

    val MOCK_DEVICES = listOf(
        BleDevice("CargoTilt-Sensor-01", "AA:BB:CC:DD:EE:01", -48),
        BleDevice("SmartHub-Relay-02", "AA:BB:CC:DD:EE:02", -67)
    )
}