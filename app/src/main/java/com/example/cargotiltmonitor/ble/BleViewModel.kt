package com.example.cargotiltmonitor.ble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargotiltmonitor.helpers.BleDevice
import com.example.cargotiltmonitor.helpers.BleHub
import com.example.cargotiltmonitor.helpers.BleLog
import com.example.cargotiltmonitor.helpers.BleProfile
import com.example.cargotiltmonitor.helpers.BleStatus
import com.example.cargotiltmonitor.helpers.IBleConnector
import com.example.cargotiltmonitor.helpers.IBleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BleViewModel : ViewModel() {
    private val connector: IBleConnector = BleHub.connector
    private val scanner: IBleScanner = BleHub.scanner

    private val _state = MutableStateFlow(BleState())
    val state: StateFlow<BleState> = _state.asStateFlow()

    private val _events = Channel<BleEvent>()
    val events = _events.receiveAsFlow()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            connector.status.collect { s -> _state.update { if (it.isScanning) it else it.copy(status = s) } }
        }
        viewModelScope.launch { connector.logs.collect { appendLog(it) } }
    }

    fun onAction(action: BleAction) {
        when (action) {
            BleAction.ScanClick -> scan()
            is BleAction.DeviceClick -> connect(action.device)
            BleAction.DisconnectClick -> connector.disconnect()
            is BleAction.SendClick -> send(action.command)
        }
    }

    private fun appendLog(line: String) =
        _state.update { it.copy(log = (it.log + line).takeLast(BleProfile.MAX_LOG_LINES)) }

    private fun message(text: String) = viewModelScope.launch { _events.send(BleEvent.ShowMessage(text)) }

    private fun scan() {
        if (!_state.value.canScan || scanJob?.isActive == true) return
        _state.update { it.copy(status = BleStatus.SCANNING, devices = emptyList()) }
        appendLog(BleLog.d("Scan started"))
        scanJob = viewModelScope.launch {
            try {
                scanner.scan().collect { d ->
                    _state.update { it.copy(devices = it.devices + d) }
                    appendLog(BleLog.d("Found ${d.name} (${d.address}) rssi=${d.rssi}"))
                }
            } finally {
                appendLog(BleLog.d("Scan finished"))
                _state.update { it.copy(status = connector.status.value) }
            }
        }
    }

    private fun connect(device: BleDevice) {
        if (!_state.value.canScan) return
        _state.update { it.copy(selected = device) }
        viewModelScope.launch {
            connector.connect(device)
            if (connector.status.value == BleStatus.CONNECTED) message("Підключено: ${device.name}")
        }
    }

    private fun send(command: String) {
        if (!_state.value.isConnected) { message("Немає з'єднання"); return }
        viewModelScope.launch { connector.sendData(command) }
    }
}