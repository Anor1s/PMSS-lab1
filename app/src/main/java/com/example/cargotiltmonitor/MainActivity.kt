package com.example.cargotiltmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cargotiltmonitor.ble.BleScreen
import com.example.cargotiltmonitor.ble.BleViewModel
import com.example.cargotiltmonitor.monitor.MonitorAction
import com.example.cargotiltmonitor.monitor.MonitorScreen
import com.example.cargotiltmonitor.monitor.MonitorViewModel

class MainActivity : ComponentActivity() {
    private val vm: MonitorViewModel by viewModels()
    private val bleVm: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppRoot(vm, bleVm) } }
    }

    override fun onResume() {
        super.onResume()
        vm.onAction(MonitorAction.Start)
    }

    override fun onPause() {
        vm.onAction(MonitorAction.Stop)
        super.onPause()
    }
}

@Composable
private fun AppRoot(vm: MonitorViewModel, bleVm: BleViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("📐") }, label = { Text("Датчик") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("📡") }, label = { Text("BLE") })
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            if (tab == 0) MonitorScreen(vm) else BleScreen(bleVm)
        }
    }
}