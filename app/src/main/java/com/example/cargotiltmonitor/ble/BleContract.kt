package com.example.cargotiltmonitor.ble

import com.example.cargotiltmonitor.helpers.BleDevice
import com.example.cargotiltmonitor.helpers.BleStatus

data class BleState(
    val status: BleStatus = BleStatus.IDLE,
    val devices: List<BleDevice> = emptyList(),
    val selected: BleDevice? = null,
    val log: List<String> = emptyList()
) {
    val isScanning get() = status == BleStatus.SCANNING
    val isConnected get() = status == BleStatus.CONNECTED || status == BleStatus.DATA_TRANSFER
    val canScan get() = status == BleStatus.IDLE || status == BleStatus.DISCONNECTED
}

sealed interface BleAction {
    data object ScanClick : BleAction
    data class DeviceClick(val device: BleDevice) : BleAction
    data object DisconnectClick : BleAction
    data class SendClick(val command: String) : BleAction
}

sealed interface BleEvent {
    data class ShowMessage(val text: String) : BleEvent
}