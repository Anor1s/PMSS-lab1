package com.example.cargotiltmonitor.analytics

data class ChartPoint(val timestamp: Long, val value: Float, val action: Boolean)

data class AnalyticsState(
    val points: List<ChartPoint> = emptyList(),
    val min: Float = 0f,
    val max: Float = 0f,
    val avg: Float = 0f,
    val actionCount: Int = 0
)

sealed interface AnalyticsAction {
    data object Clear : AnalyticsAction
}

sealed interface AnalyticsEvent {
    data class ShowMessage(val text: String) : AnalyticsEvent
}