package com.example.cargotiltmonitor.helpers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class BleStatus { IDLE, SCANNING, CONNECTING, CONNECTED, DATA_TRANSFER, DISCONNECTED }

data class BleDevice(val name: String, val address: String, val rssi: Int)

interface IBleConnector {
    val status: StateFlow<BleStatus>
    val logs: Flow<String>
    suspend fun connect(device: BleDevice)
    fun disconnect()
    suspend fun sendData(data: String)
}

interface IBleScanner {
    fun scan(): Flow<BleDevice>
}