package com.example.cargotiltmonitor.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cargotiltmonitor.data.HistoryConfig
import com.example.cargotiltmonitor.data.HistoryRepositories
import com.example.cargotiltmonitor.data.SensorRecord
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AnalyticsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = HistoryRepositories.get(app)

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _events = Channel<AnalyticsEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Room сам надсилає новий список при кожній вставці → графік оновлюється в реальному часі
        viewModelScope.launch {
            repository.observeLast(HistoryConfig.CHART_POINTS).collect { _state.value = toState(it) }
        }
    }

    // ЄДИНИЙ вхід у ViewModel
    fun onAction(action: AnalyticsAction) {
        when (action) {
            AnalyticsAction.Clear -> viewModelScope.launch {
                repository.clear()
                _events.send(AnalyticsEvent.ShowMessage("Історію очищено"))
            }
        }
    }

    private fun toState(list: List<SensorRecord>): AnalyticsState {
        if (list.isEmpty()) return AnalyticsState()
        val values = list.map { it.sensorValue }
        return AnalyticsState(
            points = list.map { ChartPoint(it.timestamp, it.sensorValue, it.actionTriggered) },
            min = values.min(),
            max = values.max(),
            avg = values.average().toFloat(),
            actionCount = list.count { it.actionTriggered }
        )
    }
}