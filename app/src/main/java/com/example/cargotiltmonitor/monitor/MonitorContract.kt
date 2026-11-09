package com.example.cargotiltmonitor.monitor

// Стан кута нахилу вантажу
enum class MonitorStatus { NORMAL, WARNING, DANGER }

// Стан хмарної синхронізації
enum class SyncStatus { IDLE, SYNCED, PENDING, ERROR }

// Віртуальні виконавчі пристрої
enum class ActuatorType { LAMP, SIREN, FLASHLIGHT }

// State — усе, що зараз видно на екрані. Тільки val, значення за замовчуванням обов'язкові.
data class MonitorState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val tilt: Float = 0f,
    val status: MonitorStatus = MonitorStatus.NORMAL,
    val isRunning: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSync: String = "",
    val pendingCount: Int = 0,
    val lamp: Boolean = false,
    val siren: Boolean = false,
    val flashlight: Boolean = false,
    val flashAvailable: Boolean = false,
    val lastCommand: String = ""
)

sealed interface MonitorAction {
    data object Start : MonitorAction
    data object Stop : MonitorAction
    data class SetActuator(val type: ActuatorType, val on: Boolean) : MonitorAction
}

sealed interface MonitorEvent {
    data class ShowMessage(val text: String) : MonitorEvent
}