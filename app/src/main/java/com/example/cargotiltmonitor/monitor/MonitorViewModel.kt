package com.example.cargotiltmonitor.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargotiltmonitor.helpers.SensorReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.acos
import kotlin.math.sqrt

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        // Порогові значення (градуси)
        const val WARNING_ANGLE = 15f
        const val DANGER_ANGLE = 30f
    }

    // Помічник, що працює з Android API
    private val sensorReader = SensorReader(app)

    // State: приватний змінюваний + публічний тільки для читання
    private val _state = MutableStateFlow(MonitorState())
    val state: StateFlow<MonitorState> = _state.asStateFlow()

    // Event: Channel доставляє кожну подію рівно один раз
    private val _events = Channel<MonitorEvent>()
    val events = _events.receiveAsFlow()

    private var sensorJob: Job? = null

    // ЄДИНИЙ вхід у ViewModel — всі дії приходять сюди
    fun onAction(action: MonitorAction) {
        when (action) {
            MonitorAction.Start -> startSensor()
            MonitorAction.Stop -> stopSensor()
        }
    }

    // Реєстрація слухача: починаємо збирати Flow з даними акселерометра
    private fun startSensor() {
        if (sensorJob != null) return
        _state.update { it.copy(isRunning = true) }
        sensorJob = viewModelScope.launch {
            sensorReader.readings().collect { v ->
                onReading(v[0], v[1], v[2])
            }
        }
    }

    // Зупинка слухача: скасування Flow → unregisterListener() (енергозбереження)
    private fun stopSensor() {
        sensorJob?.cancel()
        sensorJob = null
        _state.update { it.copy(isRunning = false) }
    }

    // Нові дані з сенсора → обчислення кута, статусу й оновлення State
    private fun onReading(x: Float, y: Float, z: Float) {
        val tilt = calculateTilt(x, y, z)
        val status = statusFor(tilt)
        val previous = _state.value.status

        _state.update { it.copy(x = x, y = y, z = z, tilt = tilt, status = status) }

        // Разова подія: тільки в момент переходу в стан НЕБЕЗПЕКА
        if (status == MonitorStatus.DANGER && previous != MonitorStatus.DANGER) {
            viewModelScope.launch {
                _events.send(MonitorEvent.ShowMessage("Небезпека! Нахил понад ${DANGER_ANGLE.toInt()}°"))
            }
        }
    }

    // Кут між вектором тяжіння та віссю Y (від вертикалі), в градусах
    private fun calculateTilt(x: Float, y: Float, z: Float): Float {
        val g = sqrt(x * x + y * y + z * z)
        return if (g > 0.1f) {
            Math.toDegrees(acos((y / g).coerceIn(-1f, 1f)).toDouble()).toFloat()
        } else 0f
    }

    // Порівняння кута з порогами
    private fun statusFor(tilt: Float): MonitorStatus = when {
        tilt < WARNING_ANGLE -> MonitorStatus.NORMAL
        tilt < DANGER_ANGLE -> MonitorStatus.WARNING
        else -> MonitorStatus.DANGER
    }
}