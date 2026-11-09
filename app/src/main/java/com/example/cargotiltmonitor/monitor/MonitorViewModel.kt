package com.example.cargotiltmonitor.monitor

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargotiltmonitor.actuators.ActuatorConfig
import com.example.cargotiltmonitor.actuators.Decision
import com.example.cargotiltmonitor.actuators.DecisionEngine
import com.example.cargotiltmonitor.actuators.SmartActuator
import com.example.cargotiltmonitor.helpers.BleHub
import com.example.cargotiltmonitor.helpers.BleProfile
import com.example.cargotiltmonitor.helpers.NetworkChecker
import com.example.cargotiltmonitor.helpers.SensorReader
import com.example.cargotiltmonitor.repository.AppRepositories
import com.example.cargotiltmonitor.repository.CloudConfig
import com.example.cargotiltmonitor.repository.RemoteCommand
import com.example.cargotiltmonitor.repository.SensorReading
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        // Порогові значення (градуси)
        const val WARNING_ANGLE = 15f
        const val DANGER_ANGLE = 30f
    }

    private val sensorReader = SensorReader(app)
    private val network = NetworkChecker(app)
    private val repository = AppRepositories.sensors
    private val actuator = SmartActuator(app)
    private val engine = DecisionEngine()

    private val _state = MutableStateFlow(MonitorState(flashAvailable = actuator.canFlash))
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    private val _events = Channel<MonitorEvent>()
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null
    private var uploadJob: Job? = null
    private var pollJob: Job? = null
    private var lastSentAt = 0L
    private var lastSentTilt = 0f
    private var autoActive = false
    private var lastRemote = RemoteCommand()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    // ЄДИНИЙ вхід у ViewModel
    fun onAction(action: MonitorAction) {
        when (action) {
            MonitorAction.Start -> startSensor()
            MonitorAction.Stop -> stopSensor()
            is MonitorAction.SetActuator -> setActuator(action.type, action.on)
        }
    }

    private fun message(text: String) = viewModelScope.launch { _events.send(MonitorEvent.ShowMessage(text)) }

    private fun startSensor() {
        if (sensorJob != null) return
        _state.update { it.copy(isRunning = true) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v -> onReading(v[0], v[1], v[2]) }
        }
        pollJob = viewModelScope.launch { pollCommands() }
    }

    // Скасування Flow → unregisterListener() (енергозбереження); актуатори вимикаються
    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        pollJob?.cancel()
        pollJob = null
        actuator.release()
        autoActive = false
        lastRemote = RemoteCommand()
        _state.update { it.copy(isRunning = false, lamp = false, siren = false, flashlight = false) }
    }

    override fun onCleared() {
        actuator.release()
    }

    private fun onReading(x: Float, y: Float, z: Float) {
        val tilt = calculateTilt(x, y, z)
        val status = statusFor(tilt)
        val previous = _state.value.status

        _state.update { it.copy(x = x, y = y, z = z, tilt = tilt, status = status) }

        // Разова подія: тільки в момент переходу в стан НЕБЕЗПЕКА
        if (status == MonitorStatus.DANGER && previous != MonitorStatus.DANGER) {
            message("Небезпека! Нахил понад ${DANGER_ANGLE.toInt()}°")
            viewModelScope.launch { BleHub.connector.sendData(BleProfile.CMD_ALARM_ON) } // команда на смарт-хаб (ЛР №2)
        }

        // Локальне автоматичне керування (ЛР №4)
        when (engine.decide(tilt, autoActive)) {
            Decision.ACTIVATE -> { autoActive = true; setAll(true) }
            Decision.RELEASE -> { autoActive = false; setAll(false) }
            Decision.KEEP -> Unit
        }

        maybeUpload(x, y, z, tilt)
    }

    private fun setAll(on: Boolean) {
        ActuatorType.entries.forEach { setActuator(it, on) }
    }

    private fun setActuator(type: ActuatorType, on: Boolean) {
        when (type) {
            ActuatorType.LAMP -> _state.update { it.copy(lamp = on) }
            ActuatorType.SIREN -> {
                if (on) actuator.triggerAlarm() else actuator.stopAlarm()
                _state.update { it.copy(siren = on) }
            }
            ActuatorType.FLASHLIGHT -> {
                val applied = actuator.toggleFlashlight(on)
                if (on && !applied && !_state.value.flashlight) message("Апаратного ліхтарика немає: лише віртуальний")
                _state.update { it.copy(flashlight = on) }
            }
        }
    }

    // Cloud-to-Device: періодичне опитування каналу команд (Polling); зупиняється скасуванням у delay()
    private suspend fun pollCommands() {
        if (CloudConfig.COMMAND_URL.contains("PASTE")) return
        while (true) {
            if (network.isOnline()) {
                repository.fetchCommand().onSuccess { applyRemote(it) }
            }
            delay(CloudConfig.POLL_INTERVAL_MS)
        }
    }

    // Застосовуємо лише ті поля, які змінились, щоб не перебивати ручне керування щоразу
    private fun applyRemote(cmd: RemoteCommand) {
        if (cmd == lastRemote) return
        if (cmd.lamp != lastRemote.lamp) cmd.lamp?.let { setActuator(ActuatorType.LAMP, it == ActuatorConfig.CMD_ON) }
        if (cmd.siren != lastRemote.siren) cmd.siren?.let { setActuator(ActuatorType.SIREN, it == ActuatorConfig.CMD_ON) }
        if (cmd.flashlight != lastRemote.flashlight) cmd.flashlight?.let { setActuator(ActuatorType.FLASHLIGHT, it == ActuatorConfig.CMD_ON) }
        lastRemote = cmd
        val text = "lamp=${cmd.lamp}, siren=${cmd.siren}, flashlight=${cmd.flashlight}"
        _state.update { it.copy(lastCommand = "$text (${timeFormat.format(Date())})") }
        message("Команда з хмари: $text")
    }

    // Тротлінг: раз на SEND_INTERVAL_MS або при зміні кута на SEND_DELTA_DEG (не частіше MIN_GAP_MS)
    private fun maybeUpload(x: Float, y: Float, z: Float, tilt: Float) {
        if (uploadJob?.isActive == true) return
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastSentAt
        val changed = abs(tilt - lastSentTilt) >= CloudConfig.SEND_DELTA_DEG
        val due = elapsed >= CloudConfig.SEND_INTERVAL_MS || (changed && elapsed >= CloudConfig.MIN_GAP_MS)
        if (!due) return

        lastSentAt = now
        lastSentTilt = tilt
        uploadJob = viewModelScope.launch { upload(SensorReading(x, y, z, tilt)) }
    }

    private suspend fun upload(reading: SensorReading) {
        if (!network.isOnline()) {
            markUnsent(SyncStatus.PENDING, "Немає інтернету: дані в статусі Pending")
            return
        }
        val result = repository.save(reading)
        if (result.isSuccess) {
            _state.update {
                it.copy(syncStatus = SyncStatus.SYNCED, lastSync = timeFormat.format(Date()), pendingCount = 0)
            }
        } else {
            markUnsent(SyncStatus.ERROR, "Помилка хмари: ${result.exceptionOrNull()?.javaClass?.simpleName}")
        }
    }

    // Повідомлення лише при зміні статусу, щоб не засмічувати екран кожні 20 с
    private fun markUnsent(status: SyncStatus, text: String) {
        val previous = _state.value.syncStatus
        _state.update { it.copy(syncStatus = status, pendingCount = it.pendingCount + 1) }
        if (previous != status) message(text)
    }

    // Кут між вектором тяжіння та віссю Y (від вертикалі), в градусах
    private fun calculateTilt(x: Float, y: Float, z: Float): Float {
        val g = sqrt(x * x + y * y + z * z)
        return if (g > 0.1f) {
            Math.toDegrees(acos((y / g).coerceIn(-1f, 1f)).toDouble()).toFloat()
        } else 0f
    }

    private fun statusFor(tilt: Float): MonitorStatus = when {
        tilt < WARNING_ANGLE -> MonitorStatus.NORMAL
        tilt < DANGER_ANGLE -> MonitorStatus.WARNING
        else -> MonitorStatus.DANGER
    }
}