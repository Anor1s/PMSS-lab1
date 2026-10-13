package com.example.cargotiltmonitor.monitor

// Стан кута нахилу вантажу
enum class MonitorStatus { NORMAL, WARNING, DANGER }

// State — усе, що зараз видно на екрані. Тільки val, значення за замовчуванням обов'язкові.
data class MonitorState(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val tilt: Float = 0f,                    // кут нахилу від вертикалі (градуси)
    val status: MonitorStatus = MonitorStatus.NORMAL,
    val isRunning: Boolean = false           // чи слухаємо сенсор
)

// Action — усі дії, які надходять у ViewModel.
sealed interface MonitorAction {
    data object Start : MonitorAction           // екран видно → слухаємо сенсор
    data object Stop : MonitorAction            // екран сховано → відписуємось
}

// Event — разові повідомлення від ViewModel до екрана.
sealed interface MonitorEvent {
    data class ShowMessage(val text: String) : MonitorEvent
}