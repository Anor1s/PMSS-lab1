package com.example.cargotiltmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.cargotiltmonitor.monitor.MonitorAction
import com.example.cargotiltmonitor.monitor.MonitorScreen
import com.example.cargotiltmonitor.monitor.MonitorViewModel

class MainActivity : ComponentActivity() {

    // Та сама ViewModel переживає поворот екрана
    private val vm: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MonitorScreen(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onAction(MonitorAction.Start)   // екран видно → слухаємо сенсор
    }

    override fun onPause() {
        vm.onAction(MonitorAction.Stop)    // екран сховано → відписуємось, економимо батарею
        super.onPause()
    }
}