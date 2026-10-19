package com.example.cargotiltmonitor.helpers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class MockBleConnector : IBleConnector {
    private val _status = MutableStateFlow(BleStatus.IDLE)
    override val status: StateFlow<BleStatus> = _status.asStateFlow()

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val logs: Flow<String> = _logs.asSharedFlow()

    private val txLock = Mutex()
    private var device: BleDevice? = null

    private fun log(msg: String) { _logs.tryEmit(BleLog.d(msg)) }

    override suspend fun connect(device: BleDevice) {
        if (_status.value == BleStatus.CONNECTING || this.device != null) return
        _status.value = BleStatus.CONNECTING
        log("GATT connect → ${device.name} (${device.address})")
        try {
            delay(BleProfile.CONNECT_DELAY_MS.milliseconds)
        } catch (e: CancellationException) {
            _status.value = BleStatus.DISCONNECTED
            throw e
        }
        this.device = device
        _status.value = BleStatus.CONNECTED
        log("Connected. Service ${BleProfile.SERVICE_UUID}")
    }

    override fun disconnect() {
        val d = device ?: return
        device = null
        _status.value = BleStatus.DISCONNECTED
        log("Disconnected from ${d.name}")
    }

    override suspend fun sendData(data: String) = txLock.withLock {
        if (device == null) { log("TX failed: not connected"); return@withLock }
        _status.value = BleStatus.DATA_TRANSFER
        val hex = data.toByteArray().joinToString(" ") { "%02X".format(it) }
        log("TX → ${BleProfile.COMMAND_CHAR_UUID} [$hex] \"$data\"")
        delay(BleProfile.WRITE_DELAY_MS.milliseconds)
        log("RX ← ${BleProfile.STATUS_CHAR_UUID} (notify) \"ACK:$data\"")
        if (device != null) _status.value = BleStatus.CONNECTED
    }
}